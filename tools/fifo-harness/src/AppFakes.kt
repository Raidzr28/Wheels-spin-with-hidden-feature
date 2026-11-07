// In-memory stand-ins for the two collaborators PickerViewModel constructs. Entry is copied
// verbatim from NameRepository.kt; storage and vibration are faked.
package com.wheelsspin.namepicker

import android.app.Application

data class Entry(val id: Long, val name: String)

class NameRepository(@Suppress("unused") context: Application) {

    private var stored: List<Entry> = listOf("Alex", "Sam", "Jordan", "Riley", "Casey", "Taylor")
        .mapIndexed { index, name -> Entry(index.toLong() + 1, name) }

    fun loadNames(): List<Entry> = stored

    fun saveNames(entries: List<Entry>) {
        stored = entries
    }

    var removeWinnerAfterPick: Boolean = false
    var soundAndHapticsEnabled: Boolean = true
}

class Buzzer(@Suppress("unused") context: Application) {
    var armedCount = 0
    var disarmedCount = 0
    var winnerCount = 0

    fun armed() { armedCount++ }
    fun disarmed() { disarmedCount++ }
    fun winner() { winnerCount++ }
}
