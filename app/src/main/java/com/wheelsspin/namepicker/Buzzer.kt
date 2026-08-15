package com.wheelsspin.namepicker

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Drives the device vibrator directly.
 *
 * Not `View.performHapticFeedback`: that routes through touch feedback, which the system silences
 * whenever "vibrate on touch" is off — a setting that ships disabled on plenty of phones. The
 * confirmation for the hidden gesture is the only signal there is, so it cannot depend on a
 * setting the user may never have turned on.
 */
class Buzzer(context: Context) {

    private val vibrator: Vibrator? = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                    as? VibratorManager
            manager?.defaultVibrator
        }
        else -> {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /** Sonification rather than touch feedback, so a silent ringer does not suppress it. */
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    val hasVibrator: Boolean
        get() = vibrator?.hasVibrator() == true

    /** One firm buzz: the held name is now pre-selected. */
    fun armed() = play(longArrayOf(0, 180))

    /** Two quick buzzes: the pre-selection was cleared. */
    fun disarmed() = play(longArrayOf(0, 70, 130, 70))

    /** Short tick when a spin finishes. */
    fun winner() = play(longArrayOf(0, 90))

    private fun play(timings: LongArray) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        runCatching {
            // Deprecated in favour of VibrationAttributes on API 33+, but this overload still
            // works everywhere down to our minSdk of 26. Kept as one path rather than branching
            // onto an API this project has no way to exercise.
            @Suppress("DEPRECATION")
            v.vibrate(VibrationEffect.createWaveform(timings, -1), attributes)
        }
    }
}
