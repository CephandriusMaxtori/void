package com.hoid.voidlauncher.core.data

import com.hoid.voidlauncher.core.system.SystemApp

/**
 * Assigns a category to an app (design doc §4.5).
 *
 * Deliberately conservative. The brief asked how aggressive first-run
 * categorisation should be, and the answer here is *as little as possible*:
 * match on well-known package prefixes first, then a small set of unambiguous
 * keywords in the label, and fall back to `OTHER`.
 *
 * The reason is asymmetric. A wrong category costs the user one misplaced icon
 * in a tab they were already looking at. A missed category costs them nothing —
 * `OTHER` still shows the app, and `ALL` is always there. So every rule below
 * has to be one where a false positive is implausible.
 *
 * Rules are matched against the *package* rather than the label wherever
 * possible. Package names are stable across locales; a user's language setting
 * must not change which tab an app lands in.
 */
object AppClassifier {

    /**
     * Package prefixes to categories.
     *
     * Ordered longest-prefix-first at match time, so a more specific prefix
     * always wins over a shorter one that happens to be its prefix.
     */
    private val PACKAGE_RULES: List<Pair<String, AppCategory>> = listOf(
        // Communication
        "com.whatsapp" to AppCategory.COMMUNICATION,
        "org.thoughtcrime.securesms" to AppCategory.COMMUNICATION,
        "com.google.android.apps.messaging" to AppCategory.COMMUNICATION,
        "org.telegram" to AppCategory.COMMUNICATION,
        "org.signal" to AppCategory.COMMUNICATION,
        "com.discord" to AppCategory.COMMUNICATION,
        "com.slack" to AppCategory.COMMUNICATION,
        "com.microsoft.teams" to AppCategory.COMMUNICATION,
        "com.zoom" to AppCategory.COMMUNICATION,
        "com.skype.raider" to AppCategory.COMMUNICATION,

        // Media
        "com.spotify" to AppCategory.MEDIA,
        "com.netflix" to AppCategory.MEDIA,
        "com.google.android.youtube" to AppCategory.MEDIA,
        "com.amazon.avod" to AppCategory.MEDIA,
        "com.google.android.apps.photos" to AppCategory.MEDIA,
        "com.android.gallery3d" to AppCategory.MEDIA,
        "com.google.android.apps.photos" to AppCategory.MEDIA,
        "com.pinterest" to AppCategory.MEDIA,

        // Productivity
        "com.google.android.gm" to AppCategory.PRODUCTIVITY,
        "com.microsoft.office" to AppCategory.PRODUCTIVITY,
        "com.google.android.calendar" to AppCategory.PRODUCTIVITY,
        "com.google.android.keep" to AppCategory.PRODUCTIVITY,
        "com.google.android.docs" to AppCategory.PRODUCTIVITY,
        "com.notion" to AppCategory.PRODUCTIVITY,
        "com.evernote" to AppCategory.PRODUCTIVITY,
        "com.todoist" to AppCategory.PRODUCTIVITY,

        // Games
        "com.supercell" to AppCategory.GAMES,
        "com.mojang" to AppCategory.GAMES,
        "com.riotgames" to AppCategory.GAMES,
        "com.king.candycrush" to AppCategory.GAMES,
        "com.dxco.pandavszombies" to AppCategory.GAMES,
        "com.nianticlabs.pokemongo" to AppCategory.GAMES,
    )

    /**
     * Label keywords, used only when no package rule matched.
     *
     * English-only and deliberately few. This is a fallback, not a heuristic
     * engine; the point is to rescue obvious cases, not to be clever.
     */
    private val LABEL_KEYWORDS: List<Pair<String, AppCategory>> = listOf(
        "maps" to AppCategory.TOOLS,
        "camera" to AppCategory.TOOLS,
        "calculator" to AppCategory.TOOLS,
        "files" to AppCategory.TOOLS,
        "store" to AppCategory.TOOLS,
        "wallet" to AppCategory.TOOLS,
        "music" to AppCategory.MEDIA,
        "podcast" to AppCategory.MEDIA,
        "browser" to AppCategory.TOOLS,
    )

    fun classify(app: SystemApp): AppCategory {
        val pkg = app.component.packageName.lowercase()

        // Longest prefix wins, so "com.google.android.apps.photos" beats a
        // hypothetical "com.google".
        val byPackage = PACKAGE_RULES
            .filter { (prefix, _) -> pkg.startsWith(prefix) }
            .maxByOrNull { (prefix, _) -> prefix.length }
            ?.second
        if (byPackage != null) return byPackage

        val label = app.label.lowercase()
        LABEL_KEYWORDS.firstOrNull { (word, _) -> label.contains(word) }
            ?.let { return it.second }

        // A preinstalled app is more likely to be a system component the user
        // cannot remove than a game or a chat app.
        if (app.isSystem) return AppCategory.SYSTEM

        return AppCategory.OTHER
    }
}
