package com.hoid.voidlauncher.core.system

import android.content.ComponentName

/**
 * An installed activity, as reported by the system.
 *
 * Deliberately carries no icon. Icons are `core:icons`' job, and this module
 * has no business knowing what a drawable is — see the dependency rules in
 * docs/architecture.md.
 *
 * [SystemApp.versionCode] is intentionally absent. Getting it means a
 * `PackageManager` call per app, and a typical device has 150–250 of them.
 * Enumerating those eagerly would put that many binder calls on the path
 * between pressing home and seeing a home screen, which is the wrong place to
 * spend the cold-start budget. [LauncherAppsSource.versionCode] fetches it on
 * demand, for the one or two apps whose icon is actually being decoded.
 */
data class SystemApp(
    val componentKey: String,
    val component: ComponentName,
    val user: UserHandleRef,
    val label: String,
    val isSystem: Boolean,
) {
    /** Flattened `package/class`, the form `LauncherApps` accepts back. */
    val flatComponent: String get() = component.flattenToString()
}

/**
 * A profile an app is installed for: the user's own, or a work profile.
 *
 * Wrapped rather than exposing `android.os.UserHandle` directly, so
 * `core:data` can store a profile as a plain `Int` serial without depending on
 * the framework. Work-profile support is a non-goal for v1, but the schema
 * carries the serial from day one — retrofitting it later means a migration on
 * every table that references an app.
 */
@JvmInline
value class UserHandleRef(val serial: Int) {
    companion object {
        /** The current user's own profile. Not a guarantee — see [of]. */
        const val CURRENT_SERIAL = 0

        val CURRENT = UserHandleRef(CURRENT_SERIAL)
    }
}
