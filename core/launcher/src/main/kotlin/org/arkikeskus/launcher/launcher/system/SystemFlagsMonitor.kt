package org.arkikeskus.launcher.launcher.system

import android.app.AlarmManager
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.arkikeskus.launcher.model.SystemFlags
import javax.inject.Inject
import javax.inject.Singleton

/** Streams a handful of status-bar flags: airplane mode, silent, bluetooth, next alarm. */
@Singleton
class SystemFlagsMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)

    val flags: Flow<SystemFlags> = callbackFlow {
        fun emit() {
            trySend(current())
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = emit()
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON),
            false,
            observer,
        )

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = emit()
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        emit()
        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }.distinctUntilChanged()

    private fun current(): SystemFlags = SystemFlags(
        airplane = Settings.Global.getInt(
            context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0,
        ) == 1,
        silent = (audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL) !=
            AudioManager.RINGER_MODE_NORMAL,
        bluetooth = isBluetoothOn(),
        alarm = alarmManager?.nextAlarmClock != null,
    )

    private fun isBluetoothOn(): Boolean = try {
        bluetoothManager?.adapter?.isEnabled == true
    } catch (e: SecurityException) {
        false
    }
}
