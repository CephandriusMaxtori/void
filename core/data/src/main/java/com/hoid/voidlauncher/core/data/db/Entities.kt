package com.hoid.voidlauncher.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A home page.
 *
 * Pages are rows rather than an implicit index so they can be added, removed,
 * and reordered without rewriting every grid item. A page count stored as a
 * preference and items keyed by an integer would force a full item migration
 * the first time a user deletes a page from the middle.
 */
@Entity(tableName = "home_pages")
data class HomePageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Display order, ascending. Renormalised on every reorder. */
    @ColumnInfo(name = "position")
    val position: Int,
)

/**
 * Anything that occupies cells on a page: an app, a folder, or a widget.
 *
 * The structural columns — [kind], [folderId], [widgetId] — exist from the
 * first schema version even though folders (M3) and widgets (M6) are not built
 * yet. A launcher table that later grows a nullable `folderId` needs a
 * migration and a full table rewrite; adding three nullable columns up front
 * costs nothing and removes that migration from the critical path.
 */
@Entity(
    tableName = "grid_items",
    foreignKeys = [
        ForeignKey(
            entity = HomePageEntity::class,
            parentColumns = ["id"],
            childColumns = ["page_id"],
            // Deleting a page must take its items with it. An orphaned item is
            // invisible in the UI but keeps a page alive in the database
            // forever, and the page count silently drifts.
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        // Every page render is "give me this page, ordered".
        Index("page_id"),
        Index("folder_id"),
    ],
)
data class GridItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "page_id")
    val pageId: Long,

    /** What kind of thing occupies these cells. */
    @ColumnInfo(name = "kind")
    val kind: Kind,

    /**
     * `LauncherAppsSource.componentKey` for an app. Null for folders and
     * widgets.
     */
    @ColumnInfo(name = "component_key")
    val componentKey: String? = null,

    @ColumnInfo(name = "x")
    val x: Int,

    @ColumnInfo(name = "y")
    val y: Int,

    @ColumnInfo(name = "span_x", defaultValue = "1")
    val spanX: Int = 1,

    @ColumnInfo(name = "span_y", defaultValue = "1")
    val spanY: Int = 1,

    /**
     * Owning folder. Null means this item sits directly on a page. Set means it
     * lives inside a folder and the x/y are irrelevant.
     */
    @ColumnInfo(name = "folder_id")
    val folderId: Long? = null,

    /** Order within a folder. */
    @ColumnInfo(name = "sort_index", defaultValue = "0")
    val sortIndex: Int = 0,

    /** `AppWidgetProvider` class name for a widget. */
    @ColumnInfo(name = "widget_id")
    val widgetId: Int? = null,
) {
    /** Stored as TEXT so the schema stays readable and migration-friendly. */
    enum class Kind { APP, FOLDER, WIDGET, SHORTCUT }
}

/**
 * Per-app user choices that the app list cannot know.
 *
 * Auto-categorisation is a guess, and a guess the user cannot correct is a bug.
 * This table is where the correction goes, and it is keyed by component *and*
 * profile so a work-profile app can be categorised independently of its
 * personal twin.
 */
@Entity(
    tableName = "app_overrides",
    primaryKeys = ["component_key", "user_serial"],
)
data class AppOverrideEntity(
    @ColumnInfo(name = "component_key")
    val componentKey: String,

    @ColumnInfo(name = "user_serial", defaultValue = "0")
    val userSerial: Int = 0,

    /** User-chosen category, overriding the automatic one. Null = automatic. */
    @ColumnInfo(name = "category")
    val category: String? = null,

    /** Renamed label. Null = use the system label. */
    @ColumnInfo(name = "label_override")
    val labelOverride: String? = null,

    /** Hidden from the drawer. Home placement is unaffected. */
    @ColumnInfo(name = "hidden", defaultValue = "0")
    val hidden: Boolean = false,
)
