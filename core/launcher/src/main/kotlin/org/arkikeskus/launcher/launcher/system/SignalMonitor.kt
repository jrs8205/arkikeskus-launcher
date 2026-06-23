package org.arkikeskus.launcher.launcher.system

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import org.arkikeskus.launcher.model.MobileStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Streams the mobile signal level (0..4) and data generation (5G/4G/3G). The generation requires
 * READ_PHONE_STATE; without it the level still shows and the generation is null.
 */
@Singleton
class SignalMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)

    private val inactive = MobileStatus(active = false, level = 0, generation = null)

    val mobile: Flow<MobileStatus> =
        if (telephonyManager == null || !hasTelephony() || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            flowOf(inactive)
        } else {
            callbackFlow {
                val executor = ContextCompat.getMainExecutor(context)
                val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                    override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                        trySend(
                            MobileStatus(
                                active = true,
                                level = signalStrength.level.coerceIn(0, 4),
                                generation = currentGeneration(),
                            ),
                        )
                    }
                }
                try {
                    telephonyManager.registerTelephonyCallback(executor, callback)
                } catch (e: SecurityException) {
                    trySend(inactive)
                }
                awaitClose { telephonyManager.unregisterTelephonyCallback(callback) }
            }.distinctUntilChanged()
        }

    private fun hasTelephony(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    private fun currentGeneration(): String? = try {
        when (telephonyManager?.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE -> "4G"
            TelephonyManager.NETWORK_TYPE_UMTS,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            -> "3G"
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
            -> "2G"
            else -> null
        }
    } catch (e: SecurityException) {
        null
    }
}
