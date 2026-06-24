package org.arkikeskus.launcher.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A persisted app shortcut placed at a free cell (page, cellX, cellY) on the home screen.
 *
 * The unique index on (page, cellX, cellY) enforces the layout invariant at the storage layer:
 * at most one shortcut per cell. Moves that would collide are resolved as a swap (see
 * [HomeItemDao.moveOrSwap]); the constraint is the last line of defence against duplicate cells.
 */
@Entity(
    tableName = "home_items",
    indices = [Index(value = ["page", "cellX", "cellY"], unique = true)],
)
data class HomeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val className: String,
    val userSerial: Long,
    val page: Int,
    val cellX: Int,
    val cellY: Int,
) {
    /** Matches AppItem.key so entities can be resolved against the live app list. */
    val key: String get() = "$packageName/$className/$userSerial"
}
