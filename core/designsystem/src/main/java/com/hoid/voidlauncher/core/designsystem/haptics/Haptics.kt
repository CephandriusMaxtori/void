package com.hoid.voidlauncher.core.designsystem.haptics

/**
 * Haptic vocabulary (design doc §5: `CLOCK_TICK` / `CONFIRM` on snap, drop,
 * toggle).
 *
 * An interface rather than a direct `Vibrator` call so the gesture code reads
 * as intent — `haptics.snap()` — and so tests can assert that a drag produced
 * a tick without a device.
 */
interface Haptics {
    /** Crossing a cell boundary during a drag. Must be cheap; fires often. */
    fun snap()

    /** An item was dropped. */
    fun drop()

    /** A toggle or selection changed. */
    fun confirm()

    /** Something was rejected. */
    fun reject()
}
