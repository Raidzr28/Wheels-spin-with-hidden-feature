# FIFO harness

Exercises the pre-selection queue in `PickerViewModel` — the `**name` commands and the order the
queued winners come out in — without a device or an Android SDK.

`src/` holds nothing but stand-ins: `Application`, `Animatable`, `mutableStateOf`/`mutableStateListOf`,
`AndroidViewModel`, and in-memory versions of `NameRepository` and `Buzzer`. The view model itself is
compiled straight from `app/src/main/java/com/wheelsspin/namepicker/PickerViewModel.kt`, unmodified,
so the harness cannot drift from what ships. It exists because the app module needs the Android SDK
to build at all, which makes an ordinary unit test useless anywhere the SDK is unavailable.

## Running

Needs `kotlinc` 2.0+ and a JDK on `PATH`:

```sh
tools/fifo-harness/run.sh
```

Every scenario prints its own PASS/FAIL and the run ends with a summary.

## What it checks

**Scenario A — 100 spins, order.** Twenty names, the queue armed through all four command forms
(exact name, wrong case, 1-based position, unique prefix) and through multi-name commands, topped
up mid-queue so later commands have to append behind earlier ones rather than replace them. Each
spin must land on the name queued next.

It also recomputes which slice sits under the 12 o'clock pointer from `rotation.value`, using the
same geometry `Wheel.kt` draws with, and requires it to match the name in the result dialog — so a
pass means the wheel really stopped there, not just that the right variable was set.

**Scenario B — 100 spins with "remove winner after pick".** Every spin deletes a name and shifts
every position behind it. The queue holds ids, so the order has to survive that.

**Scenario C — edge cases.** The 10-entry cap, all-or-nothing commands, `**` clearing the queue,
queued names deleted before their turn, and spins past the end of the queue going back to random.

**Id probe.** A regression check for entry ids: separate adds must not share an id, deleting one
name must not take others with it, and a command must land on the name it names. Ids used to come
from `System.currentTimeMillis()`, and collisions there broke all three.

## Limits

The stubbed `Animatable` jumps straight to the final angle, so spin duration, easing, and anything
about how the animation looks are out of scope — only the angle it lands on is checked. The Compose
UI is not compiled; the harness covers `PickerViewModel` alone.
