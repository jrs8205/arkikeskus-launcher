package org.arkikeskus.launcher.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReorderPlannerTest {

    private fun app(rowId: Long, x: Int, y: Int) = ReorderPlanner.Rect(rowId, 0, x, y, 1, 1)

    @Test
    fun pushesOverlappingAppToAFreeCell() {
        val items = listOf(app(1, 0, 0))
        val plan = ReorderPlanner.planFit(items, movingRowId = 99, page = 0, cellX = 0, cellY = 0, spanX = 1, spanY = 1, columns = 4, rows = 6)
        assertThat(plan).isNotNull()
        assertThat(plan!!.keys).containsExactly(1L)
        assertThat(plan[1L]).isNotEqualTo(0 to 0)
    }

    @Test
    fun resizePushesARowOfApps() {
        val items = listOf(ReorderPlanner.Rect(99, 0, 0, 0, 1, 1), app(1, 1, 0), app(2, 2, 0))
        val plan = ReorderPlanner.planFit(items, movingRowId = 99, page = 0, cellX = 0, cellY = 0, spanX = 3, spanY = 1, columns = 4, rows = 6)
        assertThat(plan).isNotNull()
        assertThat(plan!!.keys).containsExactly(1L, 2L)
        val target = setOf(0 to 0, 1 to 0, 2 to 0)
        plan.values.forEach { cell -> assertThat(cell).isNotIn(target) } // moved out of the target rect
        assertThat(plan.values.toSet()).hasSize(2) // to distinct cells
    }

    @Test
    fun returnsNullWhenThereIsNoRoom() {
        val items = (0 until 4).map { i -> app(i.toLong() + 1, i % 2, i / 2) } // fills the 2x2 grid
        val plan = ReorderPlanner.planFit(items, movingRowId = 99, page = 0, cellX = 0, cellY = 0, spanX = 2, spanY = 2, columns = 2, rows = 2)
        assertThat(plan).isNull()
    }

    @Test
    fun outOfBoundsReturnsNull() {
        val plan = ReorderPlanner.planFit(emptyList(), movingRowId = 99, page = 0, cellX = 0, cellY = 0, spanX = 5, spanY = 1, columns = 4, rows = 6)
        assertThat(plan).isNull()
    }

    @Test
    fun noOverlapReturnsEmptyPlan() {
        val items = listOf(app(1, 3, 5))
        val plan = ReorderPlanner.planFit(items, movingRowId = 99, page = 0, cellX = 0, cellY = 0, spanX = 2, spanY = 2, columns = 4, rows = 6)
        assertThat(plan).isEqualTo(emptyMap<Long, Pair<Int, Int>>())
    }
}
