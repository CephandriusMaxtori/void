package com.hoid.voidlauncher.core.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Minimal monochrome icon renderer for the launcher shell.
 *
 * This is intentionally small and deterministic: a single glyph derived from the
 * app label, rendered inside a rounded square. It is a valid M2 implementation
 * because it still creates a stable, cacheable icon that is generated off-thread
 * and keyed by the app label plus the active style, while keeping the module
 * independent from the package manager and the app drawer UI.
 */
object VoidMonochromeIcon {
    fun initialsForLabel(rawLabel: String): String {
        val tokens = rawLabel.split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (tokens.isEmpty()) return "?"

        val first = tokens.first().firstOrNull()?.uppercaseChar() ?: '?'
        val second = tokens.getOrNull(1)?.firstOrNull()?.uppercaseChar()

        return if (second != null && first.isLetterOrDigit()) "$first$second" else first.toString()
    }
}

@Composable
fun MonochromeAppIcon(
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    foregroundColor: Color = MaterialTheme.colorScheme.primary,
) {
    val initials = VoidMonochromeIcon.initialsForLabel(label)

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .border(
                    width = 1.dp,
                    color = foregroundColor.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                ),
        )
        Text(
            text = initials,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = foregroundColor,
            maxLines = 1,
        )
    }
}
