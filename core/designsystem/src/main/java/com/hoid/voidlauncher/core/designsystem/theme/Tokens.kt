package com.hoid.voidlauncher.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens (docs/design-v0.3.md §5).
 *
 * A strict black/white/gray palette with a single red accent. The accent is for
 * indicators and active states only — using it as a general highlight colour
 * would dilute the one thing that makes it read as an accent.
 */
internal object VoidPalette {
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val OffBlack = Color(0xFF0A0A0A)
    val SurfaceLight = Color(0xFFF5F5F5)
    val Muted = Color(0xFF8A8A8A)

    /** Nothing-style red. Indicators and active states only. */
    val Accent = Color(0xFFD71921)
}

/** Corner radii, in dp. Cards and sheets only — icons are masked by the system. */
internal object VoidShapes {
    val CardSmall = 20f
    val CardLarge = 28f
    val OutlineWidth = 1f
}

/**
 * Motion durations, in milliseconds. Snappy, no overshoot (docs §5).
 *
 * Exposed as named durations rather than scattered literals so the 150–250 ms
 * band stays enforceable in review.
 */
internal object VoidMotion {
    const val Fast = 150
    const val Medium = 200
    const val Slow = 250
}
