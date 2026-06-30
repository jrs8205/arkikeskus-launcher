package org.arkikeskus.launcher.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Launcher-specific dynamic colors that are NOT part of the Material 3 [androidx.compose.material3.ColorScheme]:
 * the dynamically colored battery indicator and the signal-quality bar colors from the design handoff.
 * Provided via [LocalLauncherColors].
 */
@Immutable
data class LauncherColors(
    val batteryHigh: Color,
    val batteryMedium: Color,
    val batteryLow: Color,
    /** Signal quality colors, index 0 (weakest) .. 4 (strongest). */
    val signal: List<Color>,
    val signalFaint: Color,
    val dockScrim: Color,
    val pageIndicatorActive: Color,
    val pageIndicatorInactive: Color,
) {
    /** Smooth red→yellow→green ramp: red at 0 %, yellow at the 50 % midpoint, green at 100 %. */
    fun batteryColor(percent: Int): Color {
        val p = percent.coerceIn(0, 100)
        return if (p <= 50) {
            lerp(batteryLow, batteryMedium, p / 50f)
        } else {
            lerp(batteryMedium, batteryHigh, (p - 50) / 50f)
        }
    }

    fun signalColor(level: Int): Color = signal[level.coerceIn(0, 4)]
}

internal val LightLauncherColors = LauncherColors(
    batteryHigh = Color(0xFF30D158),
    batteryMedium = Color(0xFFFFD60A),
    batteryLow = Color(0xFFFF3B30),
    signal = listOf(
        Color(0xFFFF3B30),
        Color(0xFFFF9F0A),
        Color(0xFFFFD60A),
        Color(0xFF30D158),
        Color(0xFF30D158),
    ),
    signalFaint = Color(0x73FFFFFF),
    dockScrim = Color(0x2EFFFFFF),
    pageIndicatorActive = Color(0xFFFFFFFF),
    pageIndicatorInactive = Color(0x66FFFFFF),
)

internal val DarkLauncherColors = LauncherColors(
    batteryHigh = Color(0xFF8FD89E),
    batteryMedium = Color(0xFFFFB68A),
    batteryLow = Color(0xFFFFB4AB),
    signal = listOf(
        Color(0xFFFFB4AB),
        Color(0xFFFFB4AB),
        Color(0xFFFFB68A),
        Color(0xFFC8E6A0),
        Color(0xFF8FD89E),
    ),
    signalFaint = Color(0x29FFFFFF),
    dockScrim = Color(0x12FFFFFF),
    pageIndicatorActive = Color(0xFFFFFFFF),
    pageIndicatorInactive = Color(0x4DEAF0FF),
)

val LocalLauncherColors = staticCompositionLocalOf<LauncherColors> {
    error("LauncherColors not provided. Wrap content in LauncherTheme.")
}
