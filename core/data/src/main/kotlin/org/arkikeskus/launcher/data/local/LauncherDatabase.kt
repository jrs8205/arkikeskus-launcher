package org.arkikeskus.launcher.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// v3: added the unique (page, cellX, cellY) index on home_items. The DataModule builder uses
// fallbackToDestructiveMigration, so the table is recreated rather than migrated.
@Database(entities = [HomeItemEntity::class], version = 3, exportSchema = false)
abstract class LauncherDatabase : RoomDatabase() {
    abstract fun homeItemDao(): HomeItemDao
}
