package org.arkikeskus.launcher.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import org.arkikeskus.launcher.data.local.HomeItemDao
import org.arkikeskus.launcher.data.local.HomeItemEntity
import org.arkikeskus.launcher.data.local.LauncherDatabase
import org.arkikeskus.launcher.model.AppItem
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the app shortcuts placed on the (paged, free-cell) home screen (Room). */
@Singleton
class HomeLayoutRepository @Inject constructor(
    private val db: LauncherDatabase,
    private val dao: HomeItemDao,
) {
    val homeItems: Flow<List<HomeItemEntity>> = dao.observeAll()

    suspend fun addToHome(appItem: AppItem, columns: Int) {
        if (dao.count(appItem.packageName, appItem.className, appItem.userSerial) > 0) return
        val (page, cellX, cellY) = firstFreeCell(dao.getAll(), columns)
        dao.insert(
            HomeItemEntity(
                packageName = appItem.packageName,
                className = appItem.className,
                userSerial = appItem.userSerial,
                page = page,
                cellX = cellX,
                cellY = cellY,
            ),
        )
    }

    /**
     * Moves [appItem] to (page, cellX, cellY). If the target cell is occupied by another shortcut,
     * the two swap cells. The whole thing runs in one Room transaction (via a temporary off-grid
     * slot) so the unique (page, cellX, cellY) index is never violated mid-swap and the [homeItems]
     * flow only emits the final state.
     *
     * @return `true` if the move/swap was applied, `false` if [appItem] is no longer on the home
     *   screen (e.g. removed between the drop and this call) — callers should not apply an optimistic
     *   placement in that case.
     */
    suspend fun moveItem(appItem: AppItem, page: Int, cellX: Int, cellY: Int): Boolean =
        db.withTransaction {
            val source = dao.getByKey(appItem.packageName, appItem.className, appItem.userSerial)
                ?: return@withTransaction false
            if (source.page == page && source.cellX == cellX && source.cellY == cellY) {
                return@withTransaction true
            }
            val occupant = dao.getAt(page, cellX, cellY)
            when {
                occupant == null -> dao.moveById(source.id, page, cellX, cellY)
                occupant.id == source.id -> Unit
                else -> {
                    // Park the source off-grid, slide the occupant into the vacated cell, then land
                    // the source — keeps the unique index satisfied at every step.
                    dao.moveById(source.id, TEMP_SLOT, TEMP_SLOT, TEMP_SLOT)
                    dao.moveById(occupant.id, source.page, source.cellX, source.cellY)
                    dao.moveById(source.id, page, cellX, cellY)
                }
            }
            true
        }

    /**
     * Repacks every shortcut into the first free cells of a [columns]-wide grid, preserving reading
     * order (page, then row, then column). Call this when the home column count shrinks so shortcuts
     * stored at a now-out-of-range `cellX` are pulled back on-screen; overflow flows onto later pages.
     */
    suspend fun reflow(columns: Int) {
        if (columns <= 0) return
        db.withTransaction {
            val items = dao.getAllOrdered()
            if (items.isEmpty()) return@withTransaction
            // Rebuild from scratch: clearing first frees every cell, so the sequential repack below
            // can never collide with a not-yet-moved row under the unique index.
            dao.clear()
            val slotsPerPage = columns * ROWS
            items.forEachIndexed { i, item ->
                val slot = i % slotsPerPage
                dao.insert(
                    item.copy(
                        id = 0,
                        page = i / slotsPerPage,
                        cellX = slot % columns,
                        cellY = slot / columns,
                    ),
                )
            }
        }
    }

    suspend fun removeFromHome(appItem: AppItem) {
        dao.deleteByKey(appItem.packageName, appItem.className, appItem.userSerial)
    }

    private fun firstFreeCell(items: List<HomeItemEntity>, columns: Int): Triple<Int, Int, Int> {
        val occupied = items.map { Triple(it.page, it.cellX, it.cellY) }.toHashSet()
        var page = 0
        while (true) {
            for (y in 0 until ROWS) {
                for (x in 0 until columns) {
                    if (Triple(page, x, y) !in occupied) return Triple(page, x, y)
                }
            }
            page++
        }
    }

    companion object {
        /** Rows per home page (fixed for now). */
        const val ROWS = 6

        /** Off-grid parking slot used while swapping two cells inside a transaction. */
        private const val TEMP_SLOT = -1
    }
}
