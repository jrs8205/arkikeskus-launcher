package org.arkikeskus.launcher.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.arkikeskus.launcher.designsystem.component.BatteryBar
import org.arkikeskus.launcher.designsystem.component.SignalBars
import org.arkikeskus.launcher.designsystem.theme.LocalLauncherColors
import org.arkikeskus.launcher.launcher.system.BatteryMonitor
import org.arkikeskus.launcher.launcher.system.ConnectivityMonitor
import org.arkikeskus.launcher.launcher.system.SignalMonitor
import org.arkikeskus.launcher.model.BatteryStatus
import org.arkikeskus.launcher.model.MobileStatus
import org.arkikeskus.launcher.model.WifiBand
import org.arkikeskus.launcher.model.WifiStatus
import java.util.Date
import javax.inject.Inject

/** Combined live system status driving the home status bar. */
data class StatusBarState(
    val battery: BatteryStatus = BatteryStatus(percent = 100, charging = false),
    val wifi: WifiStatus = WifiStatus(connected = false, level = 0, band = WifiBand.UNKNOWN),
    val mobile: MobileStatus = MobileStatus(active = false, level = 0, generation = null),
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    battery: BatteryMonitor,
    connectivity: ConnectivityMonitor,
    signal: SignalMonitor,
) : ViewModel() {
    val state: StateFlow<StatusBarState> =
        combine(battery.status, connectivity.wifi, signal.mobile) { b, w, m -> StatusBarState(b, w, m) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatusBarState())
}

/**
 * A slim home status bar: the clock on the left, signal (Wi-Fi when connected, else mobile) and the
 * dynamically-coloured battery on the right. Battery/signal colours come from [LocalLauncherColors]
 * (≤20% red / ≤45% orange / >45% green; signal 0–4 red→green).
 */
@Composable
fun StatusBar(modifier: Modifier = Modifier, viewModel: StatusViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalLauncherColors.current
    val time = rememberClock()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Show Wi-Fi level when connected, otherwise the mobile signal; hide when neither is active.
            val level = when {
                s.wifi.connected -> s.wifi.level
                s.mobile.active -> s.mobile.level
                else -> -1
            }
            if (level >= 0) SignalBars(level = level, activeColor = colors.signalColor(level))
            Text("${s.battery.percent}%", color = Color.White, fontSize = 13.sp)
            BatteryBar(percent = s.battery.percent, color = colors.batteryColor(s.battery.percent))
        }
    }
}

/** Current time, formatted per the device's 12/24h setting, refreshed periodically. */
@Composable
private fun rememberClock(): String {
    val context = LocalContext.current
    val fmt = remember { android.text.format.DateFormat.getTimeFormat(context) }
    var time by remember { mutableStateOf(fmt.format(Date())) }
    LaunchedEffect(Unit) {
        while (true) {
            time = fmt.format(Date())
            delay(15_000)
        }
    }
    return time
}
