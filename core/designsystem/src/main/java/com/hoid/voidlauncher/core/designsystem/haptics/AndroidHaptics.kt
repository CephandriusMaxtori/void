package com.hoid.voidlauncher.core.designsystem.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Haptics via the platform's predefined effects.
 *
 * Only four of these exist:
 * `EFFECT_TICK`, `EFFECT_CLICK`, `EFFECT_DOUBLE_CLICK`, `EFFECT_HEAVY_CLICK`.
 * There is no `EFFECT_REJECT` and no `EFFECT_LONG` on `VibrationEffect` — those
 * names belong to `HapticFeedbackConstants`, a different class, and reaching
 * for them is a compile error.
 *
 * Uses predefined effects rather than raw durations so the result follows the
 * user's system haptic settings. A launcher that hardcodes a 20 ms buzz ignores
 * the touch-feedback accessibility setting and will buzz on a device where the
 * user turned haptics off.
 */
class AndroidHaptics(context: Context) : Haptics {

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }.getOrNull()

    override fun snap() = vibrate(VibrationEffect.EFFECT_TICK)

    override fun drop() = vibrate(VibrationEffect.EFFECT_CLICK)

    override fun confirm() = vibrate(VibrationEffect.EFFECT_CLICK)

    /** Heaviest available effect. There is no dedicated "reject" effect. */
    override fun reject() = vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)

    private fun vibrate(effectId: Int) {
        val v = vibrator ?: return
        // hasVibrator() is false on plenty of real devices, and a launcher's
        // haptics are decoration rather than function. Throwing here would mean
        // a home screen that cannot start on a device with no vibrator motor.
        if (!v.hasVibrator()) return
        runCatching { v.vibrate(VibrationEffect.createPredefined(effectId)) }
            .onFailure { Log.w(TAG, "Haptic $effectId failed", it) }
    }

    private companion object {
        const val TAG = "AndroidHaptics"
    }
}
