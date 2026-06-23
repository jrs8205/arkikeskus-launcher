package org.arkikeskus.launcher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.arkikeskus.launcher.launcher.system.BatteryMonitor
import org.arkikeskus.launcher.launcher.system.ConnectivityMonitor
import org.arkikeskus.launcher.launcher.system.SignalMonitor
import org.arkikeskus.launcher.launcher.system.SystemFlagsMonitor
import org.arkikeskus.launcher.model.BatteryStatus
import org.arkikeskus.launcher.model.MobileStatus
import org.arkikeskus.launcher.model.SystemFlags
import org.arkikeskus.launcher.model.WifiBand
import org.arkikeskus.launcher.model.WifiStatus
import javax.inject.Inject

data class StatusBlockUiState(
    val battery: BatteryStatus = BatteryStatus(0, false),
    val wifi: WifiStatus = WifiStatus(false, 0, WifiBand.UNKNOWN),
    val mobile: MobileStatus = MobileStatus(false, 0, null),
    val flags: SystemFlags = SystemFlags(),
)

@HiltViewModel
class StatusBlockViewModel @Inject constructor(
    batteryMonitor: BatteryMonitor,
    connectivityMonitor: ConnectivityMonitor,
    signalMonitor: SignalMonitor,
    systemFlagsMonitor: SystemFlagsMonitor,
) : ViewModel() {

    val uiState: StateFlow<StatusBlockUiState> = combine(
        batteryMonitor.status,
        connectivityMonitor.wifi,
        signalMonitor.mobile,
        systemFlagsMonitor.flags,
    ) { battery, wifi, mobile, flags ->
        StatusBlockUiState(battery = battery, wifi = wifi, mobile = mobile, flags = flags)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatusBlockUiState(),
    )
}
