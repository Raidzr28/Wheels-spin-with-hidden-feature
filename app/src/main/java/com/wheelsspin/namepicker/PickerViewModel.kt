package com.wheelsspin.namepicker

import android.app.Application
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.security.SecureRandom

class PickerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = NameRepository(app)
    private val random = SecureRandom()
    private val buzzer = Buzzer(app)

    val names = mutableStateListOf<Entry>().apply { addAll(repo.loadNames()) }

    var isSpinning by mutableStateOf(false)
        private set

    /** Set once a spin finishes; drives the result dialog. Cleared when the dialog is dismissed. */
    var winner by mutableStateOf<Entry?>(null)
        private set

    val history = mutableStateListOf<String>()

    var removeWinnerAfterPick by mutableStateOf(repo.removeWinnerAfterPick)
        private set

    var hapticsEnabled by mutableStateOf(repo.soundAndHapticsEnabled)
        private set

    /** Current wheel angle in degrees. The wheel itself never rotates — we rotate the geometry. */
    val rotation = Animatable(0f)

    // ---------------------------------------------------------------------------------------
    // Pre-selected winner.
    //
    // Armed by typing a prefixed command into the add-name field. Held only in memory, never
    // persisted, never surfaced in the UI, and consumed one entry per spin so it cannot silently
    // affect spins beyond the queue.
    // ---------------------------------------------------------------------------------------
    private val preselected = ArrayDeque<Long>()

    /**
     * Handles a submission from the add-name field, which doubles as the command line for the
     * pre-selection. Returns true if the input was dealt with and the caller should clear the
     * field, false if it should be left in place.
     *
     * That return value is the whole confirmation scheme, and it is why this replaced the earlier
     * gesture: it is visible on screen and does not depend on the vibrator working.
     *
     * - `**Alex`, `**3` or `**al` arm a name already on the wheel — see [resolveTarget]. The field
     *   clears. A name that matches nothing leaves the text sitting there, so it looks exactly
     *   like the add button failing rather than silently doing nothing at all.
     * - `**3,1,5` queues up to [MAX_PRESELECTED] winners in order: first spin lands on the third
     *   name, the next on the first, and so on. Spins past the end of the queue are random again.
     * - `**` on its own clears the queue, and likewise only clears the field if one was set.
     * - Anything else is added as names, exactly as before.
     *
     * A queue is applied all-or-nothing. One unrecognised entry rejects the whole command and
     * leaves the previous queue untouched, so a half-applied order can never go unnoticed.
     */
    fun submitNameField(raw: String): Boolean {
        val text = raw.trim()

        if (!text.startsWith(COMMAND_PREFIX)) {
            if (text.isEmpty()) return false
            addNames(text)
            return true
        }

        val targets = text.removePrefix(COMMAND_PREFIX)
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (targets.isEmpty()) {
            if (preselected.isEmpty()) return false
            preselected.clear()
            if (hapticsEnabled) buzzer.disarmed()
            return true
        }

        if (targets.size > MAX_PRESELECTED) return false

        // Resolve every entry before touching the queue, so a rejected command changes nothing.
        val resolved = targets.map { resolveTarget(it) ?: return false }

        preselected.clear()
        resolved.forEach { preselected.addLast(it.id) }
        if (hapticsEnabled) buzzer.armed()
        return true
    }

    /**
     * Finds the entry a command refers to. Deliberately only ever returns a name that is already
     * on the wheel: letting a command add one would turn a single typo into a stray slice
     * appearing in front of everyone, which is the one failure this feature cannot afford.
     *
     * Tried in order, so the least surprising interpretation wins:
     * 1. the exact name, ignoring case — `**alex`
     * 2. a position in the list, 1-based — `**3`, which needs no spelling at all
     * 3. a prefix, but only when exactly one name starts with it — `**jo` for Jordan
     */
    private fun resolveTarget(target: String): Entry? {
        names.firstOrNull { it.name.equals(target, ignoreCase = true) }?.let { return it }

        // Checked after the exact match so a list containing a name like "7" still wins.
        target.toIntOrNull()?.let { position -> return names.getOrNull(position - 1) }

        return names.filter { it.name.startsWith(target, ignoreCase = true) }.singleOrNull()
    }

    /**
     * The slice the queue wants next, or null to spin honestly.
     *
     * Drops queued names that have since been deleted — most often by "remove winner after pick"
     * — rather than letting a stale entry silently fall back to random.
     */
    private fun peekPreselectedIndex(): Int? {
        while (preselected.isNotEmpty()) {
            val index = names.indexOfFirst { it.id == preselected.first() }
            if (index >= 0) return index
            preselected.removeFirst()
        }
        return null
    }

    /**
     * Runs one spin and records the result.
     *
     * Must be called from a composition-scoped coroutine (`rememberCoroutineScope()`), not from
     * `viewModelScope`: driving an [Animatable] needs a `MonotonicFrameClock` in the coroutine
     * context, and only a composition scope carries one.
     *
     * If the caller's scope is cancelled mid-spin — for example the user switches tabs — the
     * animation stops, no winner is recorded, and the queued order stays intact for next time.
     */
    suspend fun spin() {
        if (isSpinning || names.size < 2) return

        val sliceCount = names.size
        val sweep = 360f / sliceCount

        val queuedIndex = peekPreselectedIndex()
        val targetIndex = queuedIndex ?: random.nextInt(sliceCount)

        // Land somewhere inside the slice rather than dead centre, so a rigged spin stops at
        // an ordinary-looking offset just like an honest one.
        val offsetInSlice = 0.18f + random.nextFloat() * 0.64f
        val targetAngle = (targetIndex + offsetInSlice) * sweep
        val desiredRotation = ((360f - targetAngle) % 360f + 360f) % 360f

        // Fold the accumulated angle back into one turn first. It draws identically modulo 360,
        // but left to grow it eventually costs Float precision — measurably enough to land on the
        // wrong slice after a few thousand spins in a single session.
        rotation.snapTo(((rotation.value % 360f) + 360f) % 360f)

        val turns = SPIN_TURNS.pick()
        val base = rotation.value + turns * 360f
        var delta = desiredRotation - (base % 360f)
        if (delta < 0f) delta += 360f
        val finalRotation = base + delta

        val duration = SPIN_DURATION_MS.pick()

        // Resolved before the animation so the result matches the slice the pointer was aimed at,
        // even if the list is edited while the wheel is still turning.
        val picked = names[targetIndex]

        isSpinning = true
        try {
            rotation.animateTo(
                targetValue = finalRotation,
                animationSpec = tween(durationMillis = duration, easing = SPIN_EASING)
            )
        } finally {
            isSpinning = false
        }

        // Only consume the queue if this spin actually used it, and only now that the animation
        // has finished — an interrupted spin leaves the order intact for next time.
        if (queuedIndex != null) preselected.removeFirstOrNull()

        winner = picked
        history.add(0, picked.name)
        if (history.size > MAX_HISTORY) history.removeAt(history.lastIndex)
        if (hapticsEnabled) buzzer.winner()
    }

    fun dismissWinner() {
        val current = winner ?: return
        winner = null
        if (removeWinnerAfterPick) removeName(current.id)
    }

    fun addNames(rawBlock: String) {
        rawBlock.split('\n', ',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEachIndexed { index, name ->
                names.add(Entry(System.currentTimeMillis() + index, name))
            }
        persist()
    }

    fun renameName(id: Long, raw: String) {
        val name = raw.trim()
        if (name.isEmpty()) return
        val index = names.indexOfFirst { it.id == id }
        if (index < 0) return
        names[index] = names[index].copy(name = name)
        persist()
    }

    fun removeName(id: Long) {
        names.removeAll { it.id == id }
        preselected.removeAll { it == id }
        persist()
    }

    fun clearAll() {
        names.clear()
        preselected.clear()
        persist()
    }

    fun shuffle() {
        val shuffled = names.shuffled(java.util.Random(random.nextLong()))
        names.clear()
        names.addAll(shuffled)
        persist()
    }

    fun clearHistory() = history.clear()

    fun toggleRemoveWinnerAfterPick() {
        removeWinnerAfterPick = !removeWinnerAfterPick
        repo.removeWinnerAfterPick = removeWinnerAfterPick
    }

    fun toggleHaptics() {
        hapticsEnabled = !hapticsEnabled
        repo.soundAndHapticsEnabled = hapticsEnabled
    }

    /** A uniform pick from the range, drawn from the same source as an honest spin. */
    private fun IntRange.pick(): Int = first + random.nextInt(last - first + 1)

    private fun persist() = repo.saveNames(names.toList())

    private companion object {
        /** Marks input as a pre-selection command instead of a name to add. */
        const val COMMAND_PREFIX = "**"

        /** Longest run of winners a single command may queue. */
        const val MAX_PRESELECTED = 10

        // How long one spin runs, and how far it travels. Keep these in step: turns divided by
        // seconds is the average speed, and holding it near 1.4 turns/s is what stops a longer
        // spin from merely feeling sluggish.
        val SPIN_DURATION_MS = 6500..9000
        val SPIN_TURNS = 8..13
        const val MAX_HISTORY = 50
        val SPIN_EASING = CubicBezierEasing(0.12f, 0.72f, 0.08f, 1f)
    }
}
