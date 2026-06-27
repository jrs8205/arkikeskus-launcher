package org.arkikeskus.launcher.feature.home

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf

/** App-unique id for our single AppWidgetHost ("ARK1"). */
const val APPWIDGET_HOST_ID = 0x41524B31

/** The Activity-owned AppWidgetHost (start/stop tied to the Activity lifecycle); null outside the launcher. */
val LocalAppWidgetHost = staticCompositionLocalOf<AppWidgetHost?> { null }

/**
 * Default home-grid span for [provider]. Prefers the API 31+ cell hints; otherwise converts the
 * provider's min size (px) to dp and applies the classic `(dp + 30) / 70` heuristic. Min 1×1.
 */
fun defaultWidgetSpans(provider: AppWidgetProviderInfo, context: Context): Pair<Int, Int> {
    if (Build.VERSION.SDK_INT >= 31 && provider.targetCellWidth > 0 && provider.targetCellHeight > 0) {
        return provider.targetCellWidth to provider.targetCellHeight
    }
    val density = context.resources.displayMetrics.density
    val wDp = provider.minWidth / density
    val hDp = provider.minHeight / density
    val sx = ((wDp + 30) / 70).toInt().coerceAtLeast(1)
    val sy = ((hDp + 30) / 70).toInt().coerceAtLeast(1)
    return sx to sy
}
