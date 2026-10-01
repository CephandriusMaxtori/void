package com.hoid.voidlauncher.core.data

import android.content.Context
import android.util.Log
import com.hoid.voidlauncher.core.data.db.AppOverrideDao
import com.hoid.voidlauncher.core.data.db.AppOverrideEntity
import com.hoid.voidlauncher.core.data.db.GridItemDao
import com.hoid.voidlauncher.core.data.db.GridItemEntity
import com.hoid.voidlauncher.core.data.db.HomePageDao
import com.hoid.voidlauncher.core.data.db.HomePageEntity
import com.hoid.voidlauncher.core.data.db.VoidDatabase
import com.hoid.voidlauncher.core.system.LauncherAppsSource
import com.hoid.voidlauncher.core.system.SystemApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * The single source of truth for installed apps (design doc §4.1).
 *
 * Merges two sources that answer different questions:
 *
 * - `LauncherApps` knows **what exists**. Live, via a callback, and treated as
 *   authoritative. If it says an app is gone, it is gone.
 * - Room knows **what the user chose**: category overrides, renames, and
 *   hidden apps. Only meaningful for apps that still exist.
 *
 * The merge is a `combine` of two flows rather than a cache that gets
 * invalidated. A cache has to be invalidated correctly, and "correctly" means
 * "whenever a package is installed, uninstalled, updated, moved to another
 * profile, or has its launcher activity enabled or disabled" — a list of rules
 * that is easy to get subtly wrong. Combining two flows has no invalidation
 * logic to get wrong.
 */
class AppRepository(
    private val systemApps: LauncherAppsSource,
    private val overrideDao: AppOverrideDao,
    private val pageDao: HomePageDao,
    private val gridItemDao: GridItemDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    /**
     * Every installed app, with the user's choices applied.
     *
     * Ordered by label, case-insensitively, so the drawer reads the way people
     * expect rather than in whatever order the system happened to return. The
     * `distinctUntilChanged` matters: `LauncherApps.Callback` fires for changes
     * that do not affect us, and without it the whole drawer recomposes when
     * an app in another profile is updated.
     */
    fun observeApps(): Flow<List<AppEntry>> =
        combine(
            systemApps.observeApps(),
            overrideDao.observeAll(),
        ) { apps, overrides ->
            val byKey = overrides.associateBy { it.componentKey }
            apps.map { app -> app.toEntry(byKey[app.componentKey]) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)

    /** Apps for the drawer: everything not explicitly hidden. */
    fun observeVisibleApps(): Flow<List<AppEntry>> =
        observeApps().map { apps -> apps.filterNot { it.hidden } }

    // --- Pages ---

    fun observePages(): Flow<List<HomePage>> =
        pageDao.observeAll()
            .map { pages ->
                pages.map { page ->
                    HomePage(
                        id = page.id,
                        position = page.position,
                        items = gridItemDao.getForPage(page.id).map { it.toGridItem() },
                    )
                }
            }
            .flowOn(ioDispatcher)

    /**
     * Guarantees at least [minimum] pages exist, creating any shortfall.
     *
     * Done once at startup rather than sprinkled through the UI, because a pager
     * with zero pages has no sensible empty state and every screen would have
     * to handle it.
     */
    suspend fun ensurePages(minimum: Int = 1) = withContext(ioDispatcher) {
        val existing = pageDao.count()
        if (existing >= minimum) return@withContext

        repeat(minimum - existing) { offset ->
            pageDao.insert(HomePageEntity(position = existing + offset))
        }
        Log.i(TAG, "Created ${minimum - existing} page(s)")
    }

    suspend fun addPage(): Long? = withContext(ioDispatcher) {
        val id = pageDao.insertIfBelowLimit(HomePage.MAX_PAGES)
        if (id == null) {
            Log.w(TAG, "Refusing to exceed the ${HomePage.MAX_PAGES}-page limit")
            return@withContext null
        }
        Log.i(TAG, "Added page $id")
        id
    }

    /**
     * Removes a page, refusing to remove the last one.
     *
     * That guard is the whole point: a home screen with no pages is
     * unrecoverable without reinstalling, and a user tapping "remove" twice
     * quickly should not be able to do that to themselves.
     */
    suspend fun removePage(id: Long): Boolean = withContext(ioDispatcher) {
        if (pageDao.count() <= 1) {
            Log.w(TAG, "Refusing to remove the last page")
            return@withContext false
        }
        pageDao.deleteAndRenormalise(id)
        true
    }

    suspend fun reorderPages(orderedIds: List<Long>) = withContext(ioDispatcher) {
        pageDao.applyOrder(orderedIds)
    }

    suspend fun firstPageId(): Long = withContext(ioDispatcher) {
        pageDao.getAll().firstOrNull()?.id ?: 0L
    }

    // --- Grid items ---

    /**
     * Places an app in a cell, moving it if it is already placed elsewhere.
     *
     * Placing and moving are different operations even though the caller
     * cannot tell them apart, so this resolves which one it is first. Getting
     * that wrong is not a compile error — `move` against a row that does not
     * exist is a silent no-op, and the app simply never appears on the home
     * screen.
     */
    suspend fun placeApp(pageId: Long, componentKey: String, x: Int, y: Int) =
        withContext(ioDispatcher) {
            val existing = gridItemDao.findByComponent(componentKey)
            if (existing != null) {
                gridItemDao.move(existing.id, pageId, x, y)
            } else {
                // movingId = NO_ROW matches no real row, so the target cell is
                // cleared unconditionally before the insert.
                gridItemDao.clearCell(pageId, x, y, movingId = NO_ROW)
                gridItemDao.insert(
                    GridItemEntity(
                        pageId = pageId,
                        kind = GridItemEntity.Kind.APP,
                        componentKey = componentKey,
                        x = x,
                        y = y,
                    ),
                )
            }
            Log.d(TAG, "Placed $componentKey at ($x,$y) on page $pageId")
        }

    suspend fun moveItem(itemId: Long, pageId: Long, x: Int, y: Int) =
        withContext(ioDispatcher) {
            gridItemDao.move(itemId, pageId, x, y)
        }

    // --- Overrides ---

    suspend fun setCategory(componentKey: String, category: AppCategory?) =
        withContext(ioDispatcher) {
            overrideDao.upsert(
                currentOverride(componentKey).copy(category = category?.name),
            )
        }

    suspend fun setLabel(componentKey: String, label: String?) =
        withContext(ioDispatcher) {
            overrideDao.upsert(currentOverride(componentKey).copy(labelOverride = label))
        }

    suspend fun setHidden(componentKey: String, hidden: Boolean) =
        withContext(ioDispatcher) {
            overrideDao.upsert(currentOverride(componentKey).copy(hidden = hidden))
        }

    /**
     * Existing override for a key, or a blank one.
     *
     * A read-modify-write rather than a plain REPLACE, because setting a hidden
     * flag must not wipe a category override the user set earlier.
     */
    private suspend fun currentOverride(componentKey: String): AppOverrideEntity =
        overrideDao.getAll().firstOrNull { it.componentKey == componentKey }
            ?: AppOverrideEntity(componentKey = componentKey)

    private fun SystemApp.toEntry(override: AppOverrideEntity?): AppEntry = AppEntry(
        componentKey = componentKey,
        label = override?.labelOverride ?: label,
        category = AppCategory.fromNameOrNull(override?.category)
            ?: AppClassifier.classify(this),
        userSerial = user.serial,
        isSystem = isSystem,
        hidden = override?.hidden ?: false,
        categoryIsOverridden = override?.category != null,
    )

    companion object {
        const val TAG = "AppRepository"

        /** Sentinel that matches no row id, for "clear this cell unconditionally". */
        const val NO_ROW = 0L
    }
}

/**
 * Builds the repository with its own database.
 *
 * The database is constructed *here*, inside `core:data`, rather than being
 * handed in from `app`. Two reasons:
 *
 * 1. Layering. `core:data` owns all persistence, so the database is its own
 *    private implementation detail. Exposing `VoidDatabase` to `app` would put
 *    a DAO in the composition root and make the module boundary decorative.
 * 2. Gradle visibility. Room is an `implementation` dependency of this module,
 *    so it is absent from `app`'s compile classpath, and a caller holding a
 *    `VoidDatabase` cannot even resolve its `RoomDatabase` supertype. Not
 *    exposing the type at all is the fix that does not require `api`.
 */
fun AppRepository.Companion.create(
    context: Context,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): AppRepository {
    val appContext = context.applicationContext
    val database = VoidDatabase.build(appContext)
    return AppRepository(
        systemApps = LauncherAppsSource(appContext),
        overrideDao = database.appOverrideDao(),
        pageDao = database.homePageDao(),
        gridItemDao = database.gridItemDao(),
        ioDispatcher = ioDispatcher,
    )
}
