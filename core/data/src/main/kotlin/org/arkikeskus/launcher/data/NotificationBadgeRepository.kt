package org.arkikeskus.launcher.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the current notification-dot counts, keyed by [org.arkikeskus.launcher.model.AppItem.badgeKey]
 * ("packageName/userSerial"). The platform [android.service.notification.NotificationListenerService]
 * (in the app module) pushes snapshots here; ViewModels observe [badges] and the UI draws a dot.
 *
 * A plain singleton (not Room/DataStore): notification state is ephemeral and rebuilt on every
 * listener reconnect, so there is nothing to persist.
 */
@Singleton
class NotificationBadgeRepository @Inject constructor() {
    private val _badges = MutableStateFlow<Map<String, Int>>(emptyMap())
    val badges: StateFlow<Map<String, Int>> = _badges.asStateFlow()

    /** Replaces the full set of badge counts with a fresh snapshot from the listener. */
    fun setBadges(counts: Map<String, Int>) {
        _badges.value = counts
    }
}
