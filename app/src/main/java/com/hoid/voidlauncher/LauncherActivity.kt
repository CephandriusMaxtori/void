package com.hoid.voidlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hoid.voidlauncher.core.designsystem.theme.VoidTheme

/**
 * The single activity. Everything visible happens in Compose.
 *
 * A launcher gets an unusual lifecycle: the system starts it outside any normal
 * task, it may be killed and restarted without warning, and the user sees it on
 * every home press. The manifest carries `stateNotNeeded="true"` and a broad
 * `configChanges` set so rotation does not destroy and rebuild the process — a
 * launcher that restarts on rotation is very visible, and against a cold-start
 * budget of a few hundred milliseconds it is very obvious.
 */
class LauncherActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            VoidTheme {
                LauncherSurface()
            }
        }
    }
}

/**
 * Placeholder root for M1 scaffolding. Replaced by the real home/drawer
 * coordinator once `feature:home` and `feature:drawer` have content.
 */
@Composable
private fun LauncherSurface() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Void",
            style = MaterialTheme.typography.displayLarge,
        )
    }
}
