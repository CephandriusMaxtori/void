package com.hoid.voidlauncher.core.data

import androidx.room.Embedded
import androidx.room.Relation
import com.hoid.voidlauncher.core.data.db.AppOverrideEntity
import com.hoid.voidlauncher.core.data.db.GridItemEntity
import com.hoid.voidlauncher.core.system.SystemApp

/**
 * Automatic categories (design doc §4.5).
 *
 * Order is meaningful: it is the order the drawer's category tabs appear in.
 * `ALL` is not a category, it is the absence of a filter.
 */
enum class AppCategory(val label: String) {
    COMMUNICATION("Comm"),
    MEDIA("Media"),
    GAMES("Games"),
    TOOLS("Tools"),
    PRODUCTIVITY("Work"),
    SYSTEM("System"),
    OTHER("Other"),
    ;

    companion object {
        fun fromNameOrNull(raw: String?): AppCategory? =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    }
}

/**
 * An app, merged from the system and from the user's saved choices.
 *
 * This is the model the UI consumes. It is deliberately not a Room entity:
 * `LauncherApps` is the source of truth for *what exists*, and Room is the
 * source of truth for *what the user chose*. Merging them into one table
 * would mean a background job writing rows for every installed app on every
 * device, which is a lot of churn for data the system already knows.
 */
data class AppEntry(
    val componentKey: String,
    val label: String,
    val category: AppCategory,
    val userSerial: Int,
    val isSystem: Boolean,
    val hidden: Boolean,
    /** True when the user has explicitly overridden the category. */
    val categoryIsOverridden: Boolean = false,
)

/** One home page, resolved for rendering. */
data class HomePage(
    val id: Long,
    val position: Int,
    val items: List<GridItem>,
) {
    companion object {
        const val MAX_PAGES = 7
    }
}

/** Something occupying cells on a page. */
sealed interface GridItem {
    val id: Long
    val x: Int
    val y: Int
    val spanX: Int
    val spanY: Int

    data class App(
        override val id: Long,
        override val x: Int,
        override val y: Int,
        val componentKey: String,
        val userSerial: Int,
    ) : GridItem {
        override val spanX: Int get() = 1
        override val spanY: Int get() = 1
    }

    data class Folder(
        override val id: Long,
        override val x: Int,
        override val y: Int,
        override val spanX: Int,
        override val spanY: Int,
    ) : GridItem

    data class Widget(
        override val id: Long,
        override val x: Int,
        override val y: Int,
        override val spanX: Int,
        override val spanY: Int,
        val widgetId: Int,
    ) : GridItem
}

/**
 * Maps the storage model onto the render model.
 *
 * The profile serial is recovered from the component key rather than stored
 * separately. `componentKey` is `package/class#serial`, and the serial is part
 * of the key's identity anyway — two copies of the same app in different
 * profiles are two different keys, which is the point of it. A redundant
 * `user_serial` column on `grid_items` would be a second source of truth that
 * could disagree with the key.
 */
internal fun GridItemEntity.toGridItem(): GridItem = when (kind) {
    GridItemEntity.Kind.APP, GridItemEntity.Kind.SHORTCUT -> {
        val key = componentKey.orEmpty()
        GridItem.App(
            id = id,
            x = x,
            y = y,
            componentKey = key,
            userSerial = key.userSerialOrZero(),
        )
    }

    GridItemEntity.Kind.FOLDER -> GridItem.Folder(id, x, y, spanX, spanY)
    GridItemEntity.Kind.WIDGET -> GridItem.Widget(id, x, y, spanX, spanY, widgetId ?: 0)
}

/**
 * The profile serial embedded in a component key.
 *
 * `#` cannot appear in a Java package or class name, so it is an unambiguous
 * separator. A key without one is malformed rather than merely unusual, so this
 * falls back to the current profile instead of throwing — a single bad row
 * should not take down the home screen.
 */
internal fun String.userSerialOrZero(): Int =
    substringAfter('#', "").toIntOrNull() ?: 0
