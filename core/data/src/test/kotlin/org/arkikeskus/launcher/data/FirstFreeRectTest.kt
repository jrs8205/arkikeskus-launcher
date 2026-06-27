package org.arkikeskus.launcher.data

import com.google.common.truth.Truth.assertThat
import org.arkikeskus.launcher.data.local.HomeItemEntity
import org.junit.Test

class FirstFreeRectTest {

    private fun rect(items: List<HomeItemEntity>, columns: Int, sx: Int, sy: Int) =
        HomeLayoutRepository.firstFreeRect(items, columns, sx, sy)

    @Test
    fun `empty grid places at origin`() {
        assertThat(rect(emptyList(), columns = 4, sx = 2, sy = 2)).isEqualTo(Triple(0, 0, 0))
    }

    @Test
    fun `a 2x2 widget at origin pushes a 1x1 to the right of it`() {
        val w = HomeItemEntity(page = 0, cellX = 0, cellY = 0, spanX = 2, spanY = 2, appWidgetId = 1, widgetProvider = "p")
        assertThat(rect(listOf(w), columns = 4, sx = 1, sy = 1)).isEqualTo(Triple(0, 2, 0))
    }

    @Test
    fun `oversized span is clamped to the grid and still places`() {
        assertThat(rect(emptyList(), columns = 4, sx = 10, sy = 10)).isEqualTo(Triple(0, 0, 0))
    }

    @Test
    fun `a full page advances placement to the next page`() {
        // columns=2, ROWS=6 → a 2x6 widget fills page 0 entirely.
        val full = HomeItemEntity(page = 0, cellX = 0, cellY = 0, spanX = 2, spanY = 6, appWidgetId = 1, widgetProvider = "p")
        assertThat(rect(listOf(full), columns = 2, sx = 1, sy = 1)).isEqualTo(Triple(1, 0, 0))
    }
}
