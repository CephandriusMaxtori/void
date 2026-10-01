# R8 / ProGuard rules.
#
# Room ships its own consumer rules and needs nothing here — verified by the
# Phase 0 smoke test, which built a minified release with no extra keeps.
#
# The real risk in a launcher is the surface the *system* reflects over across
# process restarts. If any of these get stripped, the failure is not a crash —
# it is a launcher that silently stops receiving package events or widget
# updates, which is much harder to notice and diagnose. These keeps are
# deliberately conservative. Revisit in M6 once widget hosting is real, and
# consider whether each one is still load-bearing.

# --- Launcher surface ---

# The system instantiates the home activity by name from the manifest, and
# re-creates it after a process death without a saved-state restore.
-keep class com.hoid.voidlauncher.LauncherActivity { *; }

# LauncherApps.Callback is looked up reflectively by the framework when it
# reattaches a client after the process restarts.
-keep class * implements android.content.pm.LauncherApps.Callback { *; }

# AppWidgetProvider subclasses are instantiated by name from the widget picker.
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class * extends android.appwidget.AppWidgetService { *; }

# Room's generated implementations are resolved by name from the @Database
# class. Room's own rules cover this; kept explicit because a failure here is
# silent and looks like "the database is empty".
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# --- Diagnostics ---
# Keep line numbers so release crash reports are readable, but rename the
# source file attribute so the original paths are not leaked.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
