package org.arkikeskus.launcher.data

/**
 * Greedy "push others aside" planner for resizing/placing a grid item into occupied space: it relocates
 * each item the target rect would overlap to another free cell on the SAME page (largest first) and
 * reports the moves — or null if it can't be done. Pure + testable; the moving item itself is committed
 * by the caller. Adapted in spirit from AOSP Launcher3's reorder algorithm (greedy relocation, not its
 * full directional cluster push).
 */
object ReorderPlanner {

    /** A grid item's footprint: its row id, page, top-left cell and span. */
    data class Rect(
        val rowId: Long,
        val page: Int,
        val cellX: Int,
        val cellY: Int,
        val spanX: Int,
        val spanY: Int,
    )

    /**
     * Plan to fit [movingRowId]'s target rect (page, cellX.., cellY.., [spanX]×[spanY]) by relocating the
     * items it would overlap to free cells on the same [page]. Returns displaced rowId -> new
     * (cellX, cellY), an empty map if nothing needs to move, or null if it can't be done within the
     * [columns]×[rows] grid (the caller then rejects the resize/move).
     */
    fun planFit(
        items: List<Rect>,
        movingRowId: Long,
        page: Int,
        cellX: Int,
        cellY: Int,
        spanX: Int,
        spanY: Int,
        columns: Int,
        rows: Int,
    ): Map<Long, Pair<Int, Int>>? {
        if (cellX < 0 || cellY < 0 || spanX < 1 || spanY < 1) return null
        if (cellX + spanX > columns || cellY + spanY > rows) return null

        fun cells(x: Int, y: Int, sx: Int, sy: Int): List<Pair<Int, Int>> =
            (0 until sx).flatMap { dx -> (0 until sy).map { dy -> (x + dx) to (y + dy) } }

        val targetCells = cells(cellX, cellY, spanX, spanY).toHashSet()
        val pageItems = items.filter { it.page == page && it.rowId != movingRowId }
        val overlapping = pageItems.filter { r -> cells(r.cellX, r.cellY, r.spanX, r.spanY).any { it in targetCells } }
        if (overlapping.isEmpty()) return emptyMap()
        val nonOverlapping = pageItems.filterNot { it in overlapping }

        // Cells that are off-limits for relocations: the target rect + every item that stays put.
        val occupied = HashSet(targetCells)
        for (r in nonOverlapping) occupied += cells(r.cellX, r.cellY, r.spanX, r.spanY)

        val plan = LinkedHashMap<Long, Pair<Int, Int>>()
        // Largest items first — they are the hardest to re-home.
        for (item in overlapping.sortedByDescending { it.spanX * it.spanY }) {
            var placed = false
            search@ for (y in 0..rows - item.spanY) {
                for (x in 0..columns - item.spanX) {
                    val c = cells(x, y, item.spanX, item.spanY)
                    if (c.none { it in occupied }) {
                        plan[item.rowId] = x to y
                        occupied += c
                        placed = true
                        break@search
                    }
                }
            }
            if (!placed) return null
        }
        return plan
    }
}
