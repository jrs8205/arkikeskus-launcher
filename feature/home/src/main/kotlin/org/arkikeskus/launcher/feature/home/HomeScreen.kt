package org.arkikeskus.launcher.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.arkikeskus.launcher.model.AppItem
import org.arkikeskus.launcher.ui.AppActions

/**
 * Home screen: a paged [Workspace] of app shortcuts + the dock. All home gestures (icon drag, swipe
 * up/down, long-press settings, page swipe) live inside [Workspace] so they don't fight each other.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    homeSignals: Flow<Unit> = emptyFlow(),
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings
    val context = LocalContext.current
    var selectedHomeApp by remember { mutableStateOf<AppItem?>(null) }
    var selectedDockApp by remember { mutableStateOf<AppItem?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Workspace(
                pageCount = uiState.pageCount,
                columns = settings.homeColumns,
                rows = viewModel.rows,
                placedApps = uiState.placedApps,
                showLabels = settings.showHomeLabels,
                showPageIndicator = settings.showPageIndicator,
                swipeUpForDrawer = settings.swipeUpForDrawer,
                swipeDownForNotifications = settings.swipeDownForNotifications,
                homeSignals = homeSignals,
                onAppClick = viewModel::launch,
                onAppMenu = { selectedHomeApp = it },
                onMove = viewModel::moveItem,
                onOpenDrawer = onOpenDrawer,
                onOpenNotifications = { NotificationShade.expand(context) },
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            )

            if (settings.dockEnabled && uiState.dockApps.isNotEmpty()) {
                Dock(
                    apps = uiState.dockApps,
                    showLabels = settings.showDockLabels,
                    backgroundAlpha = settings.dockBackgroundOpacity,
                    onAppClick = viewModel::launch,
                    onReorder = viewModel::reorderDock,
                    onAppMenu = { selectedDockApp = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                )
            }
        }
    }

    val selected = selectedHomeApp
    if (selected != null) {
        ModalBottomSheet(onDismissRequest = { selectedHomeApp = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = selected.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                HomeActionRow(stringResource(R.string.home_remove)) {
                    viewModel.removeFromHome(selected)
                    selectedHomeApp = null
                }
                HomeActionRow(stringResource(R.string.home_add_to_dock)) {
                    viewModel.addToDock(selected)
                    selectedHomeApp = null
                }
                HomeActionRow(stringResource(R.string.app_info)) {
                    AppActions.openAppInfo(context, selected.packageName)
                    selectedHomeApp = null
                }
                HomeActionRow(stringResource(R.string.uninstall)) {
                    AppActions.uninstall(context, selected.packageName)
                    selectedHomeApp = null
                }
            }
        }
    }

    val dockSelected = selectedDockApp
    if (dockSelected != null) {
        ModalBottomSheet(onDismissRequest = { selectedDockApp = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = dockSelected.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                HomeActionRow(stringResource(R.string.dock_remove)) {
                    viewModel.removeFromDock(dockSelected)
                    selectedDockApp = null
                }
                HomeActionRow(stringResource(R.string.home_add)) {
                    viewModel.addToHome(dockSelected)
                    selectedDockApp = null
                }
                HomeActionRow(stringResource(R.string.app_info)) {
                    AppActions.openAppInfo(context, dockSelected.packageName)
                    selectedDockApp = null
                }
                HomeActionRow(stringResource(R.string.uninstall)) {
                    AppActions.uninstall(context, dockSelected.packageName)
                    selectedDockApp = null
                }
            }
        }
    }
}

@Composable
private fun HomeActionRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}
