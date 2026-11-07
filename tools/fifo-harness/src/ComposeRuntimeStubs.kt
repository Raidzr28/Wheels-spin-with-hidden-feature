// Minimal snapshot-state stand-ins. Observation is irrelevant off-device; the list/value
// semantics PickerViewModel relies on are what matter.
package androidx.compose.runtime

import kotlin.reflect.KProperty

class SnapshotStateList<T> : ArrayList<T>()

fun <T> mutableStateListOf(): SnapshotStateList<T> = SnapshotStateList()

class MutableState<T>(var value: T)

fun <T> mutableStateOf(value: T): MutableState<T> = MutableState(value)

operator fun <T> MutableState<T>.getValue(thisRef: Any?, property: KProperty<*>): T = value

operator fun <T> MutableState<T>.setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
    this.value = newValue
}
