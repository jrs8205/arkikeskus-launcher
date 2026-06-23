package org.arkikeskus.launcher.launcher.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.arkikeskus.launcher.model.WifiBand
import org.arkikeskus.launcher.model.WifiStatus
import javax.inject.Inject
import javax.inject.Singleton

/** Streams Wi-Fi connection state + signal level + band via the default-network callback. */
@Singleton
class ConnectivityMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val wifiManager = context.getSystemService(WifiManager::class.java)

    private val disconnected = WifiStatus(connected = false, level = 0, band = WifiBand.UNKNOWN)

    val wifi: Flow<WifiStatus> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(parse(caps))
            }

            override fun onLost(network: Network) {
                trySend(disconnected)
            }

            override fun onUnavailable() {
                trySend(disconnected)
            }
        }
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager?.registerNetworkCallback(request, callback)
        trySend(disconnected)
        awaitClose { connectivityManager?.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun parse(caps: NetworkCapabilities): WifiStatus {
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return disconnected
        val info = caps.transportInfo as? WifiInfo
        val rssi = info?.rssi ?: -127
        val level = (wifiManager?.calculateSignalLevel(rssi) ?: 0).coerceIn(0, 4)
        val band = when (info?.frequency ?: 0) {
            in 2401..2499 -> WifiBand.GHZ_2_4
            in 4900..5899 -> WifiBand.GHZ_5
            in 5900..7125 -> WifiBand.GHZ_6
            else -> WifiBand.UNKNOWN
        }
        return WifiStatus(connected = true, level = level, band = band)
    }
}
