package org.arkikeskus.launcher.feature.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules / cancels the daily [DriveBackupWorker] via WorkManager.
 *
 * Call [scheduleDaily] when the user enables Drive backup, and [cancel] when they disable it.
 * WorkManager deduplicates by [WORK] name so repeated calls to [scheduleDaily] are idempotent
 * (the existing work is updated to the latest constraints).
 */
object BackupScheduler {
    private const val WORK = "drive-backup-daily"

    fun scheduleDaily(context: Context) {
        val req = PeriodicWorkRequestBuilder<DriveBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }
}
