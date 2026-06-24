package org.arkikeskus.launcher.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.arkikeskus.launcher.data.AppRepository
import org.arkikeskus.launcher.data.HomeLayoutRepository
import org.arkikeskus.launcher.data.NotificationBadgeRepository
import org.arkikeskus.launcher.data.SettingsRepository
import org.arkikeskus.launcher.data.local.HomeItemEntity
import org.arkikeskus.launcher.model.AppItem
import org.arkikeskus.launcher.model.LauncherSettings
import javax.inject.Inject

/** Something placed at a free cell on a home page — an app shortcut or a folder. */
sealed interface HomeEntry {
    val page: Int
    val cellX: Int
    val cellY: Int
}

/** An app shortcut placed at a free cell on a home page. */
data class PlacedApp(
    val app: AppItem,
    override val page: Int,
    override val cellX: Int,
    override val cellY: Int,
) : HomeEntry

/** A folder placed at a free cell, holding [apps] (resolved, in order). */
data class PlacedFolder(
    val id: Long,
    val name: String,
    val apps: List<AppItem>,
    override val page: Int,
    override val cellX: Int,
    override val cellY: Int,
) : HomeEntry

data class HomeUiState(
    val settings: LauncherSettings = LauncherSettings(),
    val dockApps: List<AppItem> = emptyList(),
    val entries: List<HomeEntry> = emptyList(),
    val pageCount: Int = 1,
    val badges: Map<String, Int> = emptyMap(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appRepository: AppRepository,
    private val homeLayoutRepository: HomeLayoutRepository,
    notificationBadgeRepository: NotificationBadgeRepository,
) : ViewModel() {

    val rows: Int = HomeLayoutRepository.ROWS

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.settings,
        settingsRepository.dockFavorites,
        appRepository.apps,
        homeLayoutRepository.homeItems,
        notificationBadgeRepository.badges,
    ) { settings, favoriteKeys, apps, homeItems, badges ->
        val byKey = apps.associateBy { it.key }
        val dockApps = favoriteKeys.mapNotNull { byKey[it] }.take(settings.dockColumns)
        // Children grouped by their folder id, kept in stored order.
        val childrenByFolder = homeItems
            .filter { it.containerId != HomeItemEntity.HOME }
            .groupBy { it.containerId }
        val entries = homeItems
            .filter { it.containerId == HomeItemEntity.HOME }
            .mapNotNull { row ->
                if (row.isFolder) {
                    val folderApps = childrenByFolder[row.id].orEmpty().mapNotNull { byKey[it.key] }
                    PlacedFolder(row.id, row.folderName.orEmpty(), folderApps, row.page, row.cellX, row.cellY)
                } else {
                    byKey[row.key]?.let { PlacedApp(it, row.page, row.cellX, row.cellY) }
                }
            }
        val maxPage = entries.maxOfOrNull { it.page } ?: 0
        // Only as many pages as actually have icons (min 1). A new trailing page is offered
        // transiently by the workspace while dragging, and becomes permanent once an icon lands.
        val pageCount = (maxPage + 1).coerceAtLeast(1)
        HomeUiState(
            settings = settings,
            dockApps = dockApps,
            entries = entries,
            pageCount = pageCount,
            badges = badges,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun launch(appItem: AppItem) {
        appRepository.launch(appItem).onFailure {
            Log.w("HomeViewModel", "Failed to launch ${appItem.key}", it)
        }
    }

    fun reorderDock(newOrder: List<AppItem>) =
        viewModelScope.launch { settingsRepository.reorderVisibleDock(newOrder.map { it.key }) }

    fun removeFromHome(appItem: AppItem) =
        viewModelScope.launch { homeLayoutRepository.removeFromHome(appItem) }

    fun addToDock(appItem: AppItem) =
        viewModelScope.launch { settingsRepository.addToDock(appItem.key) }

    fun removeFromDock(appItem: AppItem) =
        viewModelScope.launch { settingsRepository.removeFromDock(appItem.key) }

    fun addToHome(appItem: AppItem) = viewModelScope.launch {
        val columns = settingsRepository.settings.first().homeColumns
        homeLayoutRepository.addToHome(appItem, columns)
    }

    /** Moves/swaps a home shortcut; returns whether the repository accepted it (see [Workspace]). */
    suspend fun moveItem(appItem: AppItem, page: Int, cellX: Int, cellY: Int): Boolean =
        homeLayoutRepository.moveItem(appItem, page, cellX, cellY)

    /** Moves/swaps a folder to a home cell (folder relocation on the grid). */
    suspend fun moveFolder(folderId: Long, page: Int, cellX: Int, cellY: Int): Boolean =
        homeLayoutRepository.moveFolder(folderId, page, cellX, cellY)

    /** Cross-surface: an icon dragged from the dock onto a home cell — place it and leave the dock. */
    fun moveToHome(appItem: AppItem, page: Int, cellX: Int, cellY: Int) = viewModelScope.launch {
        val columns = settingsRepository.settings.first().homeColumns
        if (homeLayoutRepository.placeAt(appItem, page, cellX, cellY, columns)) {
            settingsRepository.removeFromDock(appItem.key)
        }
    }

    /** Cross-surface: a home icon dragged into the dock at [index] — add to dock and leave home. */
    fun moveToDock(appItem: AppItem, index: Int) = viewModelScope.launch {
        settingsRepository.addToDockAt(appItem.key, index)
        homeLayoutRepository.removeFromHome(appItem)
    }

    /** Drop an app onto another home app → make a folder of the two. */
    fun createFolder(target: AppItem, dropped: AppItem, name: String) = viewModelScope.launch {
        homeLayoutRepository.createFolder(target, dropped, name)
    }

    /** Drop an app onto an existing folder → add it to that folder. */
    fun addToFolder(appItem: AppItem, folderId: Long) = viewModelScope.launch {
        homeLayoutRepository.addToFolder(appItem, folderId)
    }

    /** Take an app out of a folder back onto the home screen (dissolves the folder if one is left). */
    fun removeFromFolder(appItem: AppItem, folderId: Long) = viewModelScope.launch {
        val columns = settingsRepository.settings.first().homeColumns
        homeLayoutRepository.removeFromFolder(appItem, folderId, columns)
    }

    fun renameFolder(folderId: Long, name: String) =
        viewModelScope.launch { homeLayoutRepository.renameFolder(folderId, name) }
}
