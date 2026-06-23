package org.arkikeskus.launcher.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.arkikeskus.launcher.designsystem.component.BatteryBar
import org.arkikeskus.launcher.designsystem.component.SignalBars
import org.arkikeskus.launcher.designsystem.theme.LocalLauncherColors
import org.arkikeskus.launcher.model.SystemFlags
import org.arkikeskus.launcher.model.WifiBand
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The two-row status square (top-right): time + flags + battery% on row 1; short date,
 * Wi-Fi (bars + band), mobile (bars + generation, hidden in airplane mode) and a battery glyph
 * on row 2. Battery and signal colors are dynamic (see LauncherColors).
 */
@Composable
fun StatusBlock(
    modifier: Modifier = Modifier,
    viewModel: StatusBlockViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    StatusBlockContent(state = state, modifier = modifier)
}

@Composable
private fun StatusBlockContent(
    state: StatusBlockUiState,
    modifier: Modifier = Modifier,
) {
    val launcherColors = LocalLauncherColors.current
    val finnish = remember { Locale("fi", "FI") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("H.mm", finnish) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE d.M.", finnish) }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(10_000L)
        }
    }

    val batteryColor = launcherColors.batteryColor(state.battery.percent)

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = now.format(timeFormatter),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                FlagIcons(state.flags)
                Text(
                    text = "${state.battery.percent} %",
                    color = batteryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = now.format(dateFormatter),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                )
                if (state.wifi.connected) {
                    SignalCluster(
                        level = state.wifi.level,
                        color = launcherColors.signalColor(state.wifi.level),
                        label = bandLabel(state.wifi.band),
                    )
                }
                if (state.mobile.active && !state.flags.airplane) {
                    SignalCluster(
                        level = state.mobile.level,
                        color = launcherColors.signalColor(state.mobile.level),
                        label = state.mobile.generation,
                    )
                }
                BatteryBar(percent = state.battery.percent, color = batteryColor)
            }
        }
    }
}

@Composable
private fun SignalCluster(level: Int, color: Color, label: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        SignalBars(level = level, activeColor = color)
        if (label != null) {
            Text(text = label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FlagIcons(flags: SystemFlags) {
    val tint = Color.White
    val iconModifier = Modifier.size(13.dp)
    if (flags.airplane) {
        Icon(Icons.Filled.AirplanemodeActive, contentDescription = null, tint = tint, modifier = iconModifier)
    }
    if (flags.silent) {
        Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = tint, modifier = iconModifier)
    }
    if (flags.bluetooth) {
        Icon(Icons.Filled.Bluetooth, contentDescription = null, tint = tint, modifier = iconModifier)
    }
    if (flags.alarm) {
        Icon(Icons.Filled.Alarm, contentDescription = null, tint = tint, modifier = iconModifier)
    }
}

private fun bandLabel(band: WifiBand): String? = when (band) {
    WifiBand.GHZ_2_4 -> "2.4G"
    WifiBand.GHZ_5 -> "5G"
    WifiBand.GHZ_6 -> "6G"
    WifiBand.UNKNOWN -> null
}
