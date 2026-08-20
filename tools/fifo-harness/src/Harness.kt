// Drives the real PickerViewModel through 100 spins with the pre-selection queue armed, and
// checks that every spin lands on the name that was queued next, in the order it was queued.
package harness

import android.app.Application
import com.wheelsspin.namepicker.PickerViewModel
import java.util.Random
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.system.exitProcess

/** The stubbed animation never actually suspends, so a plain trampoline is enough. */
fun runSyncPublic(block: suspend () -> Unit) {
    var error: Throwable? = null
    var done = false
    block.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            done = true
            error = result.exceptionOrNull()
        }
    })
    check(done) { "spin() suspended; the stubs are supposed to complete synchronously" }
    error?.let { throw it }
}

/**
 * Which slice sits under the 12 o'clock pointer at this rotation, derived from the same geometry
 * Wheel.kt draws with: slice k spans canvas angles [-90 + rotation + k*sweep, +sweep).
 */
fun sliceUnderPointerPublic(rotation: Float, sliceCount: Int): Int {
    val sweep = 360f / sliceCount
    val u = ((-rotation % 360f) + 360f) % 360f
    return (u / sweep).toInt().coerceIn(0, sliceCount - 1)
}

private val NAMES = listOf(
    "Alex", "Bella", "Cody", "Dana", "Evan", "Fara", "Gina", "Hugo", "Ivan", "Jade",
    "Kira", "Liam", "Mona", "Nico", "Omar", "Pia", "Quinn", "Rosa", "Tara", "Umar",
)

private class Failure(val spin: Int, val kind: String, val detail: String)

fun main() {
    val rng = Random(20260820L)
    val vm = PickerViewModel(Application())

    // Replace the six default names with a 20-name wheel of unique initials, so single-letter
    // prefix commands resolve unambiguously.
    vm.clearAll()
    vm.addNames(NAMES.joinToString(","))
    check(vm.names.map { it.name } == NAMES) { "wheel setup failed: ${vm.names.map { it.name }}" }

    val expected = ArrayDeque<String>()
    val failures = mutableListOf<Failure>()
    val log = mutableListOf<String>()
    var spins = 0
    var commandsIssued = 0
    var maxQueueDepth = 0

    while (spins < 100) {
        // Arm more names whenever there is room; sometimes top up mid-queue, which is the case
        // that actually proves later commands append behind earlier ones instead of replacing.
        val room = 10 - expected.size
        if (room > 0 && (expected.isEmpty() || rng.nextInt(100) < 55)) {
            val chunk = 1 + rng.nextInt(minOf(4, room))
            val targets = (0 until chunk).map { rng.nextInt(NAMES.size) }
            val command = "**" + targets.joinToString(",") { index ->
                when (rng.nextInt(4)) {
                    0 -> NAMES[index]                       // exact name
                    1 -> NAMES[index].lowercase()           // exact name, wrong case
                    2 -> (index + 1).toString()             // 1-based position
                    else -> NAMES[index].substring(0, 1)    // unique prefix
                }
            }
            val accepted = vm.submitNameField(command)
            commandsIssued++
            if (!accepted) {
                failures.add(Failure(spins, "command rejected", command))
                continue
            }
            targets.forEach { expected.addLast(NAMES[it]) }
            maxQueueDepth = maxOf(maxQueueDepth, expected.size)
            log.add("cmd  $command  -> queue now ${expected.size}")
        }

        if (expected.isEmpty()) continue

        val want = expected.removeFirst()
        runSyncPublic { vm.spin() }
        spins++

        val got = vm.winner?.name
        if (got != want) {
            failures.add(Failure(spins, "wrong winner", "expected $want, got $got"))
        }

        // The dialog says one thing; the wheel must have stopped showing the same thing.
        val under = vm.names.getOrNull(sliceUnderPointerPublic(vm.rotation.value, vm.names.size))?.name
        if (under != got) {
            failures.add(
                Failure(spins, "pointer mismatch", "dialog $got, pointer over $under " +
                        "(rotation ${vm.rotation.value})")
            )
        }

        log.add("spin #$spins  want=$want  got=$got  pointer=$under")
        vm.dismissWinner()
    }

    println("=== Scenario A: 100 spins, queue armed via mixed command forms ===")
    println("spins            : $spins")
    println("commands issued  : $commandsIssued")
    println("max queue depth  : $maxQueueDepth")
    println("history length   : ${vm.history.size} (capped at 50 by design)")
    println("order failures   : ${failures.count { it.kind == "wrong winner" }}")
    println("pointer failures : ${failures.count { it.kind == "pointer mismatch" }}")
    println("other failures   : ${failures.count { it.kind == "command rejected" }}")
    if (failures.isNotEmpty()) {
        println("--- first 20 failures ---")
        failures.take(20).forEach { println("  spin ${it.spin}: ${it.kind}: ${it.detail}") }
    }
    println("--- first 12 events ---")
    log.take(12).forEach { println("  $it") }
    println("--- last 6 events ---")
    log.takeLast(6).forEach { println("  $it") }
    println()
    val aOk = failures.isEmpty()
    println(if (aOk) "SCENARIO A: PASS" else "SCENARIO A: FAIL")
    println()

    probeIds()
    val bOk = scenarioB()
    val cOk = scenarioC()

    println("=== Overall ===")
    println("A (100 spins, FIFO order)         : ${if (aOk) "PASS" else "FAIL"}")
    println("B (100 spins, winner removed)     : ${if (bOk) "PASS" else "FAIL"}")
    println("C (queue edge cases)              : ${if (cOk) "PASS" else "FAIL"}")

    if (!(aOk && bOk && cOk)) exitProcess(1)
}
