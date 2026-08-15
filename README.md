# Name Picker

An Android spinning-wheel name picker. Add names, hit SPIN, the wheel decelerates and lands on
one of them.

## Features

- Spinning wheel with coloured slices, per-slice labels, and a fixed pointer at 12 o'clock
- Spins run 6.5–9s over 8–13 turns; tune `SPIN_DURATION_MS` and `SPIN_TURNS` in `PickerViewModel`
- Add names one at a time or paste a comma/newline separated list
- Rename, delete, shuffle, clear all
- Names persist across restarts (SharedPreferences)
- Optional "remove winner after pick" for drawing without replacement
- Recent-picks history
- Material 3 with dynamic colour on Android 12+, light and dark
- Honest spins use `SecureRandom`

## Requirements

- Android 8.0 (API 26) or newer
- Built against SDK 35, Kotlin 2.0.21, AGP 8.7.3, Gradle 8.9

## Building

In Android Studio: open this folder and press Run.

From the command line you need a JDK 17. This machine has one at
`%LOCALAPPDATA%\Java\jdk-17.0.20+8` (the system default is Java 8, which Gradle will reject):

```
set JAVA_HOME=%LOCALAPPDATA%\Java\jdk-17.0.20+8
gradlew.bat assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

To install on a connected device (USB debugging on):

```
gradlew.bat installDebug
```

## The hidden pre-selection gesture

The app can be told in advance who the wheel should land on.

The **Add a name** field on the Names tab doubles as a command line. Anything starting with `**`
is treated as a pre-selection command instead of a name.

**The name must already be on the wheel.** Add people normally first, then arm one. A command never
adds anyone: otherwise a single typo would put a stray name on the wheel in front of everybody,
which is the one mistake this feature cannot afford.

Three ways to point at a name, tried in that order:

| You type | What happens |
| --- | --- |
| `**Alex` | Exact name, case and spaces ignored — `**alex`, `** Alex` all work. |
| `**3` | The third name in the list. No spelling at all — the safest option. |
| `**jo` | A prefix, but only if exactly one name starts with it. |
| `**3,1,5` | **A run of winners in order** — see below. |
| `**` | Clears the pre-selection. |
| `**Nobody` | Matches nothing — **the text stays in the field**. |
| `Alex` | Added to the list as a normal name. |

### Queuing a run of winners

Separate targets with commas to fix the order of the next several spins, up to **10**:

```
**3,1,5      1st spin -> 3rd name
             2nd spin -> 1st name
             3rd spin -> 5th name
             4th spin onwards -> genuinely random again
```

The three forms mix freely, so `**Alex, 4, jo` is fine. The same name may appear more than once.

A queue is **all-or-nothing**: if any entry matches nothing, the whole command is refused, the text
stays in the field, and the previous queue is left exactly as it was. A half-applied order would be
the worst outcome here — you would think you knew the next three winners and be wrong about one.

Two more things the queue survives:

- **Interrupting a spin** (switching tabs mid-turn) records no winner and consumes nothing, so the
  order stays intact.
- **"Remove winner after pick"**, which deletes names as they win. Queued entries pointing at names
  that no longer exist are dropped rather than quietly falling back to random.

**The cleared field is the confirmation.** That is the whole point of this design: it is visible on
screen and does not depend on the vibrator working. If the text is still sitting there, the command
matched nothing — check the spelling, or just use the position number instead.

Two deliberate refusals, both of which leave the text in place rather than guessing:

- An ambiguous prefix. With Sam and Sammy on the wheel, `**sa` matches neither. `**Sam` still gets
  Sam, because an exact name always beats a prefix.
- A position outside the list, like `**0` or `**99`.

A command is never added to the list, so a mistyped one cannot leave evidence behind.

**What happens next:** the next spin lands on that name. The wheel does a normal number of
turns (8–13), takes a normal amount of time (6.5–9s), and stops at a random offset inside the
slice rather than dead centre, so the result does not look placed.

**Each spin consumes one queued entry.** Once the queue runs out, spins are genuinely random again.
This is deliberate — a pre-selection that stuck around would keep quietly affecting later spins
after you had forgotten about it.

Other details that keep it out of sight:

- To anyone watching, you typed something into the add field and it did not take. There is no
  dialog, no toast, no highlight, and nothing marks the chosen name in the list.
- The armed name is held in memory only. It is never written to disk, never backed up, and is
  gone if the app is closed or the phone reboots.
- Arm and disarm also buzz the vibrator (one long, two short) when "Vibration" is ticked in the
  overflow menu, but that is a bonus signal — the field behaviour is the one to rely on.

Relevant code: `submitNameField` and `spin` in
[PickerViewModel.kt](app/src/main/java/com/wheelsspin/namepicker/PickerViewModel.kt). Change
`COMMAND_PREFIX` there if you want a different marker than `**`.

## One caveat

Using this to rig a raffle, giveaway, or any draw where people entered expecting a fair result
is fraud in most places. For classroom picks, party games, and settling who buys lunch, spin away.
