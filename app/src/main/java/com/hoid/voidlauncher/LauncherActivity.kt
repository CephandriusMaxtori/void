package com.hoid.voidlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hoid.voidlauncher.core.designsystem.theme.VoidTheme
import com.hoid.voidlauncher.di.AppContainer

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

        val container = AppContainer(this)

        setContent {
            VoidTheme {
                LauncherRoot(container = container)
            }
        }
    }
}

