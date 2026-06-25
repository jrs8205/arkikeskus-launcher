package org.arkikeskus.launcher.model

/**
 * Coil model for rendering an app icon. [themed] and [dark] are part of the model — and therefore the
 * cache key — so toggling themed icons or switching the dark/light theme re-renders the icon
 * immediately and caches each variant separately.
 *
 * [themed] only changes apps whose adaptive icon ships a monochrome layer (Android 13+); every other
 * app falls back to its normal icon, exactly like Pixel Launcher.
 */
data class IconRequest(
    val app: AppItem,
    val themed: Boolean = false,
    val dark: Boolean = false,
)
