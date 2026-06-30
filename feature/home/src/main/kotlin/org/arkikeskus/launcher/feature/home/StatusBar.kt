package org.arkikeskus.launcher.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
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
import org.arkikeskus.launcher.launcher.system.SystemFlagsMonitor
import org.arkikeskus.launcher.model.BatteryStatus
import org.arkikeskus.launcher.model.MobileStatus
import org.arkikeskus.launcher.model.SystemFlags
import org.arkikeskus.launcher.model.WifiBand
import org.arkikeskus.launcher.model.WifiStatus
import java.util.Date
import javax.inject.Inject

/** Combined live system status driving the home status bar. */
data class StatusBarState(
    val battery: BatteryStatus = BatteryStatus(percent = 100, charging = false),
    val wifi: WifiStatus = WifiStatus(connected = false, level = 0, band = WifiBand.UNKNOWN),
    val mobile: MobileStatus = MobileStatus(active = false, level = 0, generation = null),
    val flags: SystemFlags = SystemFlags(),
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    battery: BatteryMonitor,
    connectivity: ConnectivityMonitor,
    signal: SignalMonitor,
    flags: SystemFlagsMonitor,
) : ViewModel() {
    val state: StateFlow<StatusBarState> =
        combine(battery.status, connectivity.wifi, signal.mobile, flags.flags) { b, w, m, f ->
            StatusBarState(b, w, m, f)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatusBarState())
}

private val STATUS_EDGE_PAD = 28.dp // clock / battery inset from the screen edges
private val STATUS_GAP = 8.dp // space between indicators
private val STATUS_MAIN_LINE = 20.dp // common centre line for every primary glyph

/**
 * A slim home status bar. Clock on the left; on the right, every currently-active phone indicator —
 * status flags (alarm, DND, silent/vibrate, location, NFC, data-saver, Bluetooth, VPN, airplane),
 * the mobile signal (bars + 5G/4G/LTE), Wi-Fi and the battery (bar + percentage). Two-part readouts
 * (mobile, battery) stack vertically to save width; every primary glyph shares one centre line.
 *
 * A custom layout places the clock at the left edge and the indicators from the right edge inward,
 * flowing **around a top display cutout** (punch-hole) — when a glyph would land under the cutout it
 * jumps to the cutout's left side instead of being hidden behind it.
 *
 * Colours are dynamic: battery + signal/Wi-Fi use the vivid quality ramp from [LocalLauncherColors]
 * (≤20% red / ≤45% yellow / >45% green; signal 0–4 red→green); binary flags follow the Material You
 * accent. A subtle top scrim keeps every glyph legible on bright wallpapers.
 */
@Composable
fun StatusBar(
    modifier: Modifier = Modifier,
    alignToCutout: Boolean = false,
    viewModel: StatusViewModel = hiltViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val colors = LocalLauncherColors.current
    val accent = MaterialTheme.colorScheme.primary
    val time = rememberClock()
    val view = LocalView.current

    // The top punch-hole rect (px): left/right let the row flow around it; the vertical centre lets the
    // glyphs sit level with the camera. Null when there is no cutout.
    var cutout by remember { mutableStateOf<android.graphics.Rect?>(null) }
    LaunchedEffect(view) {
        val r = view.rootWindowInsets?.displayCutout?.boundingRectTop
        cutout = if (r != null && r.width() > 0) r else null
    }

    val f = s.flags
    val batteryColor = colors.batteryColor(s.battery.percent)

    Layout(
        modifier = modifier
            .fillMaxWidth()
            // No single colour contrasts with every wallpaper, so back the bar with a faint dark scrim
            // that fades downward — guarantees the icons stay readable on bright backgrounds.
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.30f), Color.Transparent))),
        content = {
            // [0] clock (always, far left)
            MainLineBox { Text(time, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium) }

            // [1..] active indicators, declared left → right (battery declared last = placed far right).
            if (f.alarm) FlagItem(R.drawable.ic_status_alarm, accent, "Alarm")
            if (f.dnd) FlagItem(R.drawable.ic_status_dnd, accent, "Do not disturb")
            if (f.silent) FlagItem(R.drawable.ic_status_silent, accent, "Silent")
            if (f.vibrate) FlagItem(R.drawable.ic_status_vibrate, accent, "Vibrate")
            if (f.location) FlagItem(R.drawable.ic_status_location, accent, "Location")
            if (f.nfc) FlagItem(R.drawable.ic_status_nfc, accent, "NFC")
            if (f.dataSaver) FlagItem(R.drawable.ic_status_datasaver, accent, "Data saver")
            if (f.bluetooth) FlagItem(R.drawable.ic_status_bluetooth, accent, "Bluetooth")
            if (f.vpn) FlagItem(R.drawable.ic_status_vpn, accent, "VPN")
            if (f.airplane) FlagItem(R.drawable.ic_status_airplane, accent, "Airplane mode")

            // Mobile signal (hidden in airplane mode): bars with the generation (5G/4G/LTE) stacked under.
            if (s.mobile.active && !f.airplane) {
                StatusItem(sub = s.mobile.generation, subColor = colors.signalColor(s.mobile.level)) {
                    SignalBars(level = s.mobile.level, activeColor = colors.signalColor(s.mobile.level))
                }
            }

            // Wi-Fi: a single glyph tinted by signal strength.
            if (s.wifi.connected) {
                MainLineBox {
                    Icon(
                        painterResource(R.drawable.ic_status_wifi),
                        "Wi-Fi",
                        Modifier.size(16.dp),
                        tint = colors.signalColor(s.wifi.level),
                    )
                }
            }

            // Battery (always, far right): bar + charging bolt, with the percentage stacked underneath.
            StatusItem(sub = "${s.battery.percent}%", subColor = batteryColor) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    if (s.battery.charging) {
                        Icon(
                            painterResource(R.drawable.ic_status_charging),
                            "Charging",
                            Modifier.size(11.dp),
                            tint = batteryColor,
                        )
                    }
                    BatteryBar(percent = s.battery.percent, color = batteryColor)
                }
            }
        },
    ) { measurables, constraints ->
        val edge = STATUS_EDGE_PAD.roundToPx()
        val gap = STATUS_GAP.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = constraints.maxWidth
        val contentH = placeables.maxOf { it.height }
        val clock = placeables.first()
        val icons = placeables.drop(1)
        val cut = cutout
        val cutLeft = cut?.left ?: -1
        val cutRight = cut?.right ?: -1
        // Drop the primary line so its centre sits level with the camera cutout's vertical centre.
        val topY = if (alignToCutout && cut != null) {
            ((cut.top + cut.bottom) / 2 - STATUS_MAIN_LINE.roundToPx() / 2).coerceAtLeast(0)
        } else {
            0
        }

        layout(width, topY + contentH) {
            clock.placeRelative(edge, topY)
            // Fill from the right edge inward; jump over the cutout when a glyph would land under it.
            var x = width - edge
            for (p in icons.asReversed()) {
                var left = x - p.width
                if (cut != null && left < cutRight && x > cutLeft) {
                    x = cutLeft
                    left = x - p.width
                }
                p.placeRelative(left, topY)
                x = left - gap
            }
        }
    }
}

/** A primary glyph centred on the shared main line (so battery bar, Wi-Fi, signal and flags align). */
@Composable
private fun MainLineBox(content: @Composable () -> Unit) {
    Box(Modifier.height(STATUS_MAIN_LINE), contentAlignment = Alignment.Center) { content() }
}

/** A two-part indicator: the primary glyph on the main line, a small [sub] label stacked beneath it. */
@Composable
private fun StatusItem(sub: String?, subColor: Color, primary: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MainLineBox(primary)
        if (sub != null) {
            Text(sub, color = subColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, lineHeight = 10.sp)
        }
    }
}

/** A single binary status-flag icon on the main line. */
@Composable
private fun FlagItem(@DrawableRes res: Int, tint: Color, desc: String) {
    MainLineBox {
        Icon(painterResource(res), desc, Modifier.size(15.dp), tint = tint)
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
