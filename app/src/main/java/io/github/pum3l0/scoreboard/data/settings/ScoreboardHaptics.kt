package io.github.pum3l0.scoreboard.data.settings

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings

class ScoreboardHaptics(context: Context) {
    private val resolver = context.contentResolver
    private val vibrator = context.getSystemService(Vibrator::class.java)

    @Suppress("DEPRECATION")
    fun pulse(level: HapticLevel) {
        val motor = vibrator ?: return
        if (level == HapticLevel.OFF || !motor.hasVibrator()) return
        if (Settings.System.getInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 0) return
        val amplitude = if (motor.hasAmplitudeControl()) level.amplitude else VibrationEffect.DEFAULT_AMPLITUDE
        val effect = VibrationEffect.createOneShot(level.durationMillis, amplitude)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            motor.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            motor.vibrate(effect, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
        }
    }
}
