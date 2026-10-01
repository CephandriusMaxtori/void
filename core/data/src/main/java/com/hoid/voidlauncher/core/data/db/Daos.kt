package com.hoid.voidlauncher.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HomePageDao {

    @Query("SELECT * FROM home_pages ORDER BY position")
    fun observeAll(): Flow<List<HomePageEntity>>

    @Query("SELECT * FROM home_pages ORDER BY position")
    suspend fun getAll(): List<HomePageEntity>

    @Query("SELECT COUNT(*) FROM home_pages")
    suspend fun count(): Int

    @Insert
    suspend fun insert(page: HomePageEntity): Long

    @Transaction
    suspend fun insertIfBelowLimit(maxPages: Int): Long? {
        val currentCount = count()
        if (currentCount >= maxPages) return null
        return insert(HomePageEntity(position = currentCount))
    }

    @Update
    suspend fun update(page: HomePageEntity)

    @Query("DELETE FROM home_pages WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE home_pages SET position = :position WHERE id = :id")
    suspend fun setPosition(position: Int, id: Long)

    /**
     * Rewrites positions to match the given order.
     *
     * A single transaction: renumbering row by row would leave duplicate
     * positions partway through, and a reader observing mid-transaction would
     * see pages in the wrong order.
     */
    @Transaction
    suspend fun applyOrder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setPosition(index, id) }
    }

    /**
     * Removes a page and closes the gap it leaves.
     *
     * Grid items go via `ON DELETE CASCADE` on the foreign key. Deleting them
     * here as well would be redundant; deleting them *instead* of relying on
     * the cascade would let a future code path orphan rows silently.
     */
    @Transaction
    suspend fun deleteAndRenormalise(id: Long) {
        val remaining = getAll().filter { it.id != id }
        delete(id)
        applyOrder(remaining.map { it.id })
    }
}

@Dao
interface GridItemDao {

    /** Everything placed directly on a page, in cell order. */
    @Query(
        """
        SELECT * FROM grid_items
        WHERE page_id = :pageId AND folder_id IS NULL
        ORDER BY y, x
        """,
    )
    fun observeForPage(pageId: Long): Flow<List<GridItemEntity>>

    @Query(
        """
        SELECT * FROM grid_items
        WHERE page_id = :pageId AND folder_id IS NULL
        ORDER BY y, x
        """,
    )
    suspend fun getForPage(pageId: Long): List<GridItemEntity>

    /** Contents of a folder, in user-defined order. */
    @Query("SELECT * FROM grid_items WHERE folder_id = :folderId ORDER BY sort_index")
    fun observeForFolder(folderId: Long): Flow<List<GridItemEntity>>

    @Query("SELECT * FROM grid_items WHERE folder_id = :folderId ORDER BY sort_index")
    suspend fun getForFolder(folderId: Long): List<GridItemEntity>

    @Query("SELECT * FROM grid_items WHERE component_key = :componentKey LIMIT 1")
    suspend fun findByComponent(componentKey: String): GridItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GridItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<GridItemEntity>)

    @Update
    suspend fun update(item: GridItemEntity)

    @Query("DELETE FROM grid_items WHERE id = :id")
    suspend fun delete(id: Long)

    /** Empties one cell so something else can claim it. */
    @Query(
        """
        DELETE FROM grid_items
        WHERE page_id = :pageId AND folder_id IS NULL
          AND x = :x AND y = :y
          AND id != :movingId
        """,
    )
    suspend fun clearCell(pageId: Long, x: Int, y: Int, movingId: Long)

    @Query("UPDATE grid_items SET page_id = :pageId, x = :x, y = :y WHERE id = :id")
    suspend fun placeAt(pageId: Long, x: Int, y: Int, id: Long)

    /**
     * Moves an item to a cell, displacing whatever was there.
     *
     * Clear-then-place, in one transaction. The order matters: moving *into* an
     * occupied cell has to vacate it first, or the grid ends up with two items
     * claiming the same coordinates and one of them silently vanishes from the
     * layout.
     */
    @Transaction
    suspend fun move(itemId: Long, pageId: Long, x: Int, y: Int) {
        clearCell(pageId, x, y, itemId)
        placeAt(pageId, x, y, itemId)
    }
}

@Dao
interface AppOverrideDao {

    @Query("SELECT * FROM app_overrides")
    fun observeAll(): Flow<List<AppOverrideEntity>>

    @Query("SELECT * FROM app_overrides")
    suspend fun getAll(): List<AppOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(override: AppOverrideEntity)
}
