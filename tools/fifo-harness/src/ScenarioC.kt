// Edge cases around the queue: all-or-nothing commands, the clear command, and what happens to a
// queued name that is deleted before its turn.
package harness

import android.app.Application
import com.wheelsspin.namepicker.PickerViewModel

fun scenarioC(): Boolean {
    val results = mutableListOf<Pair<String, Boolean>>()
    fun check(label: String, ok: Boolean) = results.add(label to ok)

    fun freshVm(): PickerViewModel = PickerViewModel(Application()).apply {
        clearAll()
        addNames("Alex,Bella,Cody,Dana,Evan,Fara")
    }

    // Queue caps at 10 and rejects an overflowing command whole.
    freshVm().let { vm ->
        check("10 queued accepted", vm.submitNameField("**1,2,3,4,5,6,1,2,3,4"))
        check("11th rejected", !vm.submitNameField("**5"))
        val order = (1..10).map { runSyncNamed(vm) }
        check("first 10 in order", order == listOf(
            "Alex", "Bella", "Cody", "Dana", "Evan", "Fara", "Alex", "Bella", "Cody", "Dana"))
    }

    // One bad entry rejects the whole command and leaves the queue untouched.
    freshVm().let { vm ->
        vm.submitNameField("**Cody")
        check("mixed good/bad rejected", !vm.submitNameField("**Alex,Nobody,Bella"))
        check("queue unchanged after reject", runSyncNamed(vm) == "Cody")
    }

    // "**" clears, and only reports handled when there was something to clear.
    freshVm().let { vm ->
        check("clear on empty returns false", !vm.submitNameField("**"))
        vm.submitNameField("**Fara,Evan")
        check("clear on armed returns true", vm.submitNameField("**"))
        // A cleared queue means random, and random can still hit Fara — so judge it over a run.
        val after = (1..40).map { runSyncNamed(vm) }
        check("after clear the spins are random", after.distinct().size > 1)
    }

    // A queued name deleted before its turn is skipped, not silently turned into a random spin.
    freshVm().let { vm ->
        vm.submitNameField("**Bella,Dana")
        vm.removeName(vm.names.first { it.name == "Bella" }.id)
        check("deleted head skipped, next honoured", runSyncNamed(vm) == "Dana")
    }

    // A spin past the end of the queue is random again, not a repeat of the last winner.
    freshVm().let { vm ->
        vm.submitNameField("**Evan")
        check("queued spin lands", runSyncNamed(vm) == "Evan")
        val after = (1..40).map { runSyncNamed(vm) }
        check("spins past the queue vary", after.distinct().size > 1)
    }

    println("=== Scenario C: queue edge cases ===")
    results.forEach { (label, ok) -> println("  ${if (ok) "PASS" else "FAIL"}  $label") }
    val ok = results.all { it.second }
    println(if (ok) "SCENARIO C: PASS" else "SCENARIO C: FAIL")
    println()
    return ok
}

private fun runSyncNamed(vm: PickerViewModel): String? {
    runSyncPublic { vm.spin() }
    val name = vm.winner?.name
    vm.dismissWinner()
    return name
}
