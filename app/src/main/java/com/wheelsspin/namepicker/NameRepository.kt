package com.wheelsspin.namepicker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A single wheel entry. [id] is stable so the wheel can track a name across edits. */
data class Entry(val id: Long, val name: String)

/**
 * Persists the name list and the user's preferences in SharedPreferences.
 *
 * Deliberately stores nothing about a pre-selected winner — that state lives only in memory,
 * so it never survives an app restart and never appears in a backup.
 */
class NameRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadNames(): List<Entry> {
        val raw = prefs.getString(KEY_NAMES, null) ?: return defaultNames()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Entry(obj.getLong("id"), obj.getString("name"))
            }
        }.getOrElse { defaultNames() }
    }

    fun saveNames(entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(JSONObject().put("id", entry.id).put("name", entry.name))
        }
        prefs.edit().putString(KEY_NAMES, array.toString()).apply()
    }

    var removeWinnerAfterPick: Boolean
        get() = prefs.getBoolean(KEY_REMOVE_AFTER_PICK, false)
        set(value) = prefs.edit().putBoolean(KEY_REMOVE_AFTER_PICK, value).apply()

    var soundAndHapticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTICS, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTICS, value).apply()

    private fun defaultNames(): List<Entry> =
        listOf("Alex", "Sam", "Jordan", "Riley", "Casey", "Taylor")
            .mapIndexed { index, name -> Entry(index.toLong() + 1, name) }

    private companion object {
        const val PREFS = "name_picker_prefs"
        const val KEY_NAMES = "names"
        const val KEY_REMOVE_AFTER_PICK = "remove_after_pick"
        const val KEY_HAPTICS = "haptics"
    }
}
