// Not a FIFO test: this is the regression check for the entry-id collision the queue tripped over.
package harness

import android.app.Application
import com.wheelsspin.namepicker.PickerViewModel

fun probeIds() {
    println("=== Probe: entry id uniqueness (regression) ===")

    // 1. Two adds inside the same millisecond produce the same id.
    PickerViewModel(Application()).let { vm ->
        vm.clearAll()
        vm.addNames("Alex")
        vm.addNames("Bella")
        vm.addNames("Cody")
        println("  three separate adds -> ids ${vm.names.map { it.id }}")
        println("  distinct ids: ${vm.names.map { it.id }.distinct().size} of ${vm.names.size}")
        val before = vm.names.map { it.name }
        vm.removeName(vm.names.first { it.name == "Bella" }.id)
        println("  deleting Bella: $before -> ${vm.names.map { it.name }}")
    }

    // 2. A pasted block reserves one id per name, so a later add collides for as many
    //    milliseconds as the block was long.
    PickerViewModel(Application()).let { vm ->
        vm.clearAll()
        vm.addNames((1..20).joinToString(",") { "P$it" })   // ids T .. T+19
        Thread.sleep(5)
        vm.addNames("Late")                                  // id ~T+5 -> collides with P6
        val late = vm.names.first { it.name == "Late" }
        val clash = vm.names.filter { it.id == late.id }.map { it.name }
        println("  paste of 20 then one add 5 ms later -> shared id: $clash")

        // The queue stores ids, so a rigged pick resolves to whichever entry holds that id first.
        vm.submitNameField("**Late")
        runSyncPublic { vm.spin() }
        println("  queued **Late -> spin landed on ${vm.winner?.name}")
        vm.dismissWinner()
    }
    println()
}
