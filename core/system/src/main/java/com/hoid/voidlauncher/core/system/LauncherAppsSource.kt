package com.hoid.voidlauncher.core.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/**
 * The single door to the system's view of installed apps.
 *
 * Everything else in the app consumes this rather than `LauncherApps` directly,
 * which is what keeps `core:system` swappable and confines widget-hosting
 * crashes to one module (docs/architecture.md, dependency rule 4).
 *
 * The returned flow never completes and never throws. A launcher that stops
 * receiving package events is a launcher whose home screen silently rots as the
 * user installs and uninstalls things, which is a far worse failure than a
 * visible error.
 */
class LauncherAppsSource(context: Context) {

    private val appContext: Context = context.applicationContext
    private val launcherApps = appContext.getSystemService(LauncherApps::class.java)
    private val userManager = appContext.getSystemService(UserManager::class.java)
    private val packageManager = appContext.packageManager

    /**
     * Live list of installed launchable activities.
     *
     * Backed by a [LauncherApps.Callback], so a fresh list is pushed on every
     * install, uninstall, update, or enabled-state change. The re-derivation is
     * deliberately full rather than incremental: a launcher that patches its
     * state on callbacks accumulates drift, and the drift shows up as a home
     * screen with ghost icons that no amount of restarting will clear.
     *
     * [conflate] drops intermediate emissions. Enumeration is cheap but not
     * free, and during a bulk install the user gains nothing from watching the
     * list settle.
     */
    fun observeApps(): Flow<List<SystemApp>> = callbackFlow {
        // See PackageChangeCallback for why the callback is a named class and
        // why subclassing it needs a constructor call.
        val callback = PackageChangeCallback { trySend(readAllApps()) }

        launcherApps.registerCallback(callback)
        // trySend, not send: the initial read happens off the collector's
        // suspension point and must never block the registering thread.
        trySend(readAllApps())

        awaitClose { launcherApps.unregisterCallback(callback) }
    }.conflate()

    /**
     * Enumerates every launchable activity across every profile this user can
     * see.
     *
     * A profile we cannot read is skipped rather than fatal. Losing a work
     * profile's apps degrades the drawer; throwing here would take down the
     * home screen entirely.
     */
    private fun readAllApps(): List<SystemApp> {
        val ownUser = Process.myUserHandle()
        val users = try {
            launcherApps.profiles
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot enumerate profiles", e)
            listOf(ownUser)
        }

        return buildList {
            for (user in users) {
                val ref = userRefFor(user)
                val activities = try {
                    launcherApps.getActivityList(null, user)
                } catch (e: SecurityException) {
                    Log.w(TAG, "Cannot read profile ${ref.serial}", e)
                    continue
                } catch (e: RuntimeException) {
                    // Some OEM package managers throw here rather than
                    // returning empty. Losing one profile is survivable.
                    Log.w(TAG, "Profile ${ref.serial} enumeration failed", e)
                    continue
                }

                for (info in activities) {
                    val name = info.componentName
                    add(
                        SystemApp(
                            componentKey = componentKey(name.packageName, name.className, ref.serial),
                            component = name,
                            user = ref,
                            label = info.label?.toString().orEmpty(),
                            isSystem = info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                        ),
                    )
                }
            }
        }
    }

    /**
     * Version code for one app, fetched on demand.
     *
     * Part of the icon cache key. Only called for icons actually being decoded,
     * which is a handful rather than the full installed set.
     */
    fun versionCode(componentKey: String): Long? {
        val packageName = componentKey.substringBefore('/')
        return try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
        } catch (e: PackageManager.NameNotFoundException) {
            // Uninstalled between enumeration and icon load. The icon pipeline
            // treats null as "do not cache this".
            null
        }
    }

    /**
     * Launches an app by its component key.
     *
     * The UI holds component keys, not [SystemApp]s — it renders from a merged
     * list that deliberately does not carry framework types. Resolution happens
     * here, so an app uninstalled between the list being drawn and the tap
     * returns false rather than throwing `ActivityNotFoundException`.
     *
     * Builds the intent from the exact [ComponentName] in the key rather than
     * asking the system to resolve "the main activity for this package". The
     * enumerated component *is* the LAUNCHER-category activity the user tapped.
     *
     * Returns false rather than throwing: a launch failure is a dead end for one
     * icon, not a reason to tear down the home screen.
     */
    fun launchByKey(componentKey: String, sourceBounds: Rect? = null): Boolean {
        val packageName = componentKey.substringBefore('/')
        val className = componentKey.substringAfter('/').substringBefore('#')
        val serial = componentKey.substringAfter('#', "").toIntOrNull() ?: 0

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(packageName, className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            sourceBounds?.let {
                putExtra(EXTRA_BOUNDS, it)
                addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            }
        }

        return try {
            appContext.startActivity(intent)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Not allowed to launch $componentKey", e)
            false
        } catch (e: android.content.ActivityNotFoundException) {
            Log.w(TAG, "No launchable activity for $componentKey", e)
            false
        }
    }

    /** Launches an already-resolved app. */
    fun launch(app: SystemApp, sourceBounds: Rect? = null): Boolean =
        launchByKey(app.componentKey, sourceBounds)

    private fun userRefFor(user: UserHandle): UserHandleRef = UserHandleRef(
        // UserManager uses a long serial; the handle we carry is an Int. Narrowing
        // is safe because a serial beyond Int range is not a profile this app can
        // meaningfully address.
        userManager.getSerialNumberForUser(user).toInt(),
    )

    companion object {
        private const val TAG = "LauncherAppsSource"

        /**
         * `android.content.pm.extra.EXTRA_BOUNDS`, spelled out.
         *
         * Lets a launched app size its activity to the icon that was tapped
         * instead of opening full-screen. The literal is the inter-app contract;
         * referencing the constant would tie this call site to an import that is
         * easy to get wrong by omission.
         */
        const val EXTRA_BOUNDS = "android.content.pm.extra.EXTRA_BOUNDS"

        /**
         * Stable key for an app in a given profile.
         *
         * `componentKey` is the flattened `package/class`; the serial is
         * appended because the same app can legitimately appear in both the
         * personal and work profile, and a key that ignored the profile would
         * make the two collide.
         */
        fun componentKey(
            packageName: String,
            className: String,
            userSerial: Int,
        ): String = "$packageName/$className#$userSerial"
    }
}
