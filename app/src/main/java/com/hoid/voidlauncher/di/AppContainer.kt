package com.hoid.voidlauncher.di

import android.content.Context
import com.hoid.voidlauncher.core.data.AppRepository
import com.hoid.voidlauncher.core.data.create
import com.hoid.voidlauncher.core.designsystem.haptics.AndroidHaptics
import com.hoid.voidlauncher.core.designsystem.haptics.Haptics
import com.hoid.voidlauncher.core.icons.IconLoader
import com.hoid.voidlauncher.core.system.LauncherAppsSource

/**
 * The object graph, assembled by hand.
 *
 * Every dependency is created here and nowhere else. Nothing below `app` knows
 * how to build its own collaborators; they receive them as constructor
 * parameters. That is what makes the ViewModels and repositories testable with
 * plain fakes and no test-runner infrastructure.
 *
 * All properties are `by lazy` on purpose. Constructing a repository eagerly
 * would mean opening a database and querying the package manager on the main
 * thread during `Application.onCreate`, which is the single easiest way to blow
 * a cold-start budget. For a launcher, where the user presses home and expects
 * a screen *now*, that is the one path worth being precious about.
 *
 * Note what is *not* here: the Room database and its DAOs. `core:data` builds
 * and owns those behind `AppRepository.create`, so this module never sees a
 * persistence type. That keeps the boundary real and avoids needing Room on
 * `app`'s compile classpath.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    // --- core:system ---

    val launcherApps: LauncherAppsSource by lazy { LauncherAppsSource(appContext) }

    // --- core:icons ---

    val iconLoader: IconLoader by lazy { IconLoader(appContext, launcherApps) }

    // --- core:data ---

    val appRepository: AppRepository by lazy { AppRepository.create(appContext) }

    // --- core:designsystem ---

    val haptics: Haptics by lazy { AndroidHaptics(appContext) }
}
