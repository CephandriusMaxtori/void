package com.hoid.voidlauncher.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HomePageEntity::class,
        GridItemEntity::class,
        AppOverrideEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class VoidDatabase : RoomDatabase() {

    abstract fun homePageDao(): HomePageDao
    abstract fun gridItemDao(): GridItemDao
    abstract fun appOverrideDao(): AppOverrideDao

    companion object {
        private const val NAME = "void.db"

        fun build(context: Context): VoidDatabase =
            Room.databaseBuilder(context, VoidDatabase::class.java, NAME)
                // WAL lets the UI keep reading the layout while a drag writes
                // to it, which is the common case: the home screen is
                // observable and writes on every cell drop.
                //
                // Note: Room turns on `PRAGMA foreign_keys` itself when the
                // schema declares foreign keys, so the page -> grid item
                // CASCADE does apply. Do not "fix" this by adding a manual
                // pragma or an enableMultiInstanceInvalidation call.
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}
