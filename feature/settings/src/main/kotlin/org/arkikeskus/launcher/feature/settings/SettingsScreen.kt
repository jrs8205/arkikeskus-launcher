package org.arkikeskus.launcher.feature.settings

import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            SectionTitle(stringResource(R.string.settings_gestures))
            SwitchRow(stringResource(R.string.settings_swipe_up_drawer), s.swipeUpForDrawer, viewModel::setSwipeUp)
            SwitchRow(stringResource(R.string.settings_swipe_down_notif), s.swipeDownForNotifications, viewModel::setSwipeDown)

            SectionTitle(stringResource(R.string.settings_dock))
            SwitchRow(stringResource(R.string.settings_dock_show), s.dockEnabled, viewModel::setDockEnabled)
            StepperRow(stringResource(R.string.settings_dock_icons), s.dockColumns, 3, 7, viewModel::setDockColumns)
            SwitchRow(stringResource(R.string.settings_show_labels), s.showDockLabels, viewModel::setShowDockLabels)
            SliderRow(stringResource(R.string.settings_dock_bg), s.dockBackgroundOpacity, viewModel::setDockBackgroundOpacity)

            SectionTitle(stringResource(R.string.settings_drawer))
            StepperRow(stringResource(R.string.settings_columns), s.drawerColumns, 3, 7, viewModel::setDrawerColumns)
            SwitchRow(stringResource(R.string.settings_show_labels), s.showDrawerLabels, viewModel::setShowDrawerLabels)

            SectionTitle(stringResource(R.string.settings_home))
            StepperRow(stringResource(R.string.settings_columns), s.homeColumns, 3, 7, viewModel::setHomeColumns)
            SwitchRow(stringResource(R.string.settings_show_labels), s.showHomeLabels, viewModel::setShowHomeLabels)
            SwitchRow(stringResource(R.string.settings_page_indicator), s.showPageIndicator, viewModel::setShowPageIndicator)

            SectionTitle(stringResource(R.string.settings_notifications))
            SwitchRow(stringResource(R.string.settings_notif_dots), s.showNotificationDots, viewModel::setShowNotificationDots)
            SwitchRow(stringResource(R.string.settings_notif_count), s.notificationDotCount, viewModel::setNotificationDotCount)
            ActionRow(
                label = stringResource(R.string.settings_notif_access),
                description = stringResource(R.string.settings_notif_access_desc),
            ) {
                openNotificationAccess(context)
            }
        }
    }
}

/** Opens the system notification-access screen (deep-linked to this app when supported). */
private fun openNotificationAccess(context: android.content.Context) {
    val component = ComponentName(
        context.packageName,
        "org.arkikeskus.launcher.notifications.NotificationDotListenerService",
    )
    val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val opened = runCatching { context.startActivity(detail) }.isSuccess
    if (!opened) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(label: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SliderRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..1f)
    }
}

@Composable
private fun StepperRow(label: String, value: Int, min: Int, max: Int, onValueChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = { if (value > min) onValueChange(value - 1) }) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = "$value",
            modifier = Modifier.widthIn(min = 24.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        IconButton(onClick = { if (value < max) onValueChange(value + 1) }) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}
