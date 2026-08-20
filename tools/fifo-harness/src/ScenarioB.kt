// 100 more spins with "remove winner after pick" on, so every spin shrinks the wheel and shifts
// every position behind it. The queue is keyed by id, so the order must survive that.
package harness

import android.app.Application
import com.wheelsspin.namepicker.PickerViewModel
import java.util.Random

fun scenarioB(): Boolean {
    val rng = Random(4242L)
    val vm = PickerViewModel(Application())
    vm.clearAll()
    vm.addNames(NAMES_B.joinToString(","))
    if (!vm.removeWinnerAfterPick) vm.toggleRemoveWinnerAfterPick()

    val expected = ArrayDeque<Long>()          // queued entry ids, in the order they were armed
    val expectedNames = ArrayDeque<String>()
    val failures = mutableListOf<String>()
    var spins = 0
    var refills = 0

    while (spins < 100) {
        // Keep the wheel populated: removal eats a name every spin. No pause between refills --
        // with ids from a counter rather than the clock, back-to-back adds no longer collide.
        if (vm.names.size < 12) {
            vm.addNames((0 until 8).joinToString(",") { "R${refills++}" })
        }

        val room = 10 - expected.size
        if (room > 0 && (expected.isEmpty() || rng.nextInt(100) < 50)) {
            val chunk = 1 + rng.nextInt(minOf(3, room))
            // Only target names not already queued, so a removed winner can't be queued twice.
            val candidates = vm.names.filter { it.id !in expected }
            if (candidates.size >= chunk) {
                val picks = candidates.shuffled(rng).take(chunk)
                val command = "**" + picks.joinToString(",") { entry ->
                    // Exact name, or 1-based position — no prefixes here, refill names collide.
                    if (rng.nextBoolean()) entry.name
                    else (vm.names.indexOfFirst { it.id == entry.id } + 1).toString()
                }
                if (!vm.submitNameField(command)) {
                    failures.add("spin $spins: command rejected: $command")
                } else {
                    picks.forEach { expected.addLast(it.id); expectedNames.addLast(it.name) }
                }
            }
        }

        if (expected.isEmpty()) continue

        expected.removeFirst()
        val wantName = expectedNames.removeFirst()
        runSyncPublic { vm.spin() }
        spins++

        val got = vm.winner?.name
        if (got != wantName) failures.add("spin $spins: expected $wantName, got $got")

        val under = vm.names.getOrNull(sliceUnderPointerPublic(vm.rotation.value, vm.names.size))?.name
        if (under != got) failures.add("spin $spins: pointer over $under, dialog said $got")

        vm.dismissWinner()   // removes the winner, shifting every position behind it
        if (vm.names.any { it.name == got }) failures.add("spin $spins: winner $got not removed")
    }

    println("=== Scenario B: 100 spins with 'remove winner after pick' on ===")
    println("spins            : $spins")
    println("refills added    : $refills")
    println("failures         : ${failures.size}")
    failures.take(10).forEach { println("  $it") }
    println(if (failures.isEmpty()) "SCENARIO B: PASS" else "SCENARIO B: FAIL")
    println()
    return failures.isEmpty()
}

private val NAMES_B = listOf(
    "Alex", "Bella", "Cody", "Dana", "Evan", "Fara", "Gina", "Hugo", "Ivan", "Jade",
    "Kira", "Liam", "Mona", "Nico", "Omar", "Pia", "Quinn", "Rosa", "Tara", "Umar",
)
