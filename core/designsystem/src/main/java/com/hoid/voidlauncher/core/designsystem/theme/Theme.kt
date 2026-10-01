package com.hoid.voidlauncher.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * The app theme.
 *
 * Dark is the intended default — the design is built around pure black — but
 * light is provided because a launcher that renders white-on-white against a
 * light wallpaper is unusable, and the system setting is not ours to override.
 */
@Composable
fun VoidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = VoidPalette.Accent,
            onPrimary = VoidPalette.White,
            background = VoidPalette.Black,
            onBackground = VoidPalette.White,
            surface = VoidPalette.Black,
            onSurface = VoidPalette.White,
            surfaceVariant = VoidPalette.OffBlack,
            onSurfaceVariant = VoidPalette.Muted,
            outline = VoidPalette.Muted,
        )
    } else {
        lightColorScheme(
            primary = VoidPalette.Accent,
            onPrimary = VoidPalette.White,
            background = VoidPalette.SurfaceLight,
            onBackground = VoidPalette.OffBlack,
            surface = VoidPalette.SurfaceLight,
            onSurface = VoidPalette.OffBlack,
            surfaceVariant = VoidPalette.White,
            onSurfaceVariant = VoidPalette.Muted,
            outline = VoidPalette.Muted,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        // Doto arrives in M2 alongside its licence attribution. Until then the
        // platform default is honest about not being the real thing.
        typography = Typography(),
        content = content,
    )
}

/**
 * Placeholder for the dot-matrix typeface.
 *
 * Doto is licensed under the SIL Open Font License and ships with its own
 * attribution regardless of this project's licence. It is wired in M2, when
 * the clock lands.
 */
internal val DotMatrixFallback = FontFamily.Default
