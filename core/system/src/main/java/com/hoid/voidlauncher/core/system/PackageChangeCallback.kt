package com.hoid.voidlauncher.core.system

import android.content.pm.LauncherApps
import android.os.UserHandle

/**
 * Reacts to any change in the installed-app set by re-reading it.
 *
 * Every callback funnels into one [onChanged] rather than trying to patch the
 * list incrementally. An incremental update has to get right the interaction
 * between install, uninstall, update, launcher-activity enable/disable, and
 * profile changes; getting it subtly wrong leaves ghost icons on the home
 * screen that survive a reboot.
 *
 * Note the trailing `()` when subclassing: [LauncherApps.Callback] is an
 * abstract *class* with a public constructor, not an interface. Written
 * interface-style, Kotlin reports "This type has a constructor, so it must be
 * initialized here" and never mentions LauncherApps.
 */
internal class PackageChangeCallback(
    private val onChanged: () -> Unit,
) : LauncherApps.Callback() {

    override fun onPackageRemoved(packageName: String?, user: UserHandle?) = onChanged()

    override fun onPackageAdded(packageName: String?, user: UserHandle?) = onChanged()

    override fun onPackageChanged(packageName: String?, user: UserHandle?) = onChanged()

    override fun onPackagesAvailable(
        packageNames: Array<out String>?,
        user: UserHandle?,
        replacing: Boolean,
    ) = onChanged()

    override fun onPackagesUnavailable(
        packageNames: Array<out String>?,
        user: UserHandle?,
        replacing: Boolean,
    ) = onChanged()
}
