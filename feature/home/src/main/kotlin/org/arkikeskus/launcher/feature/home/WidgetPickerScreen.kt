package org.arkikeskus.launcher.feature.home

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

/** Full-screen picker: installed widget providers grouped by app; tap one to add it. */
@Composable
fun WidgetPickerScreen(
    onPick: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    val context = LocalContext.current
    val pm = context.packageManager
    val groups = remember {
        AppWidgetManager.getInstance(context).installedProviders
            .groupBy { it.provider.packageName }
            .map { (_, providers) ->
                val appLabel = providers.first().loadLabel(pm)
                appLabel to providers.sortedBy { it.loadLabel(pm) }
            }
            .sortedBy { it.first.lowercase() }
    }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Text(
                text = stringResource(R.string.widget_picker_title),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                groups.forEach { (appLabel, providers) ->
                    item(key = "h-$appLabel") {
                        Text(
                            text = appLabel,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(providers.size) { i ->
                        WidgetRow(provider = providers[i], onClick = { onPick(providers[i]) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetRow(provider: AppWidgetProviderInfo, onClick: () -> Unit) {
    val context = LocalContext.current
    val pm = context.packageManager
    val (sx, sy) = remember(provider) { defaultWidgetSpans(provider, context) }
    val preview = remember(provider) {
        val d = runCatching { provider.loadPreviewImage(context, 0) }.getOrNull()
            ?: runCatching { provider.loadIcon(context, 0) }.getOrNull()
        d?.let { runCatching { it.toBitmap().asImageBitmap() }.getOrNull() }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (preview != null) {
            Image(bitmap = preview, contentDescription = null, modifier = Modifier.size(56.dp))
        }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(provider.loadLabel(pm), color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "${sx}×${sy}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}
