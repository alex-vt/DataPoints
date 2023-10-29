package com.alexvt.datapoints.entrypoints

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import com.alexvt.datapoints.R
import com.alexvt.datapoints.datacollection.recordDataPoint
import com.alexvt.datapoints.datacollection.scheduleSingleCollectionAtNextMinuteStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DataPointCollectionTimeEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        scheduleSingleCollectionAtNextMinuteStart(context)

        val isFilesystemAccessible =
            Environment.isExternalStorageManager()
        val isPowerSaving =
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode
        val isIneligibleForDataPointCollection =
            !isFilesystemAccessible || isPowerSaving
        if (isIneligibleForDataPointCollection) {
            return
        }

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            Intent(context, DataPointCollectionForegroundService::class.java).run {
                context.startForegroundService(this)
                val recordingStartTimeMillis = System.currentTimeMillis()
                recordDataPoint(recordingStartTimeMillis, context)
                val minUserInteractionDelayMillis = 5000L
                val remainingUserInteractionDelay = minUserInteractionDelayMillis -
                        (System.currentTimeMillis() - recordingStartTimeMillis)
                delay(remainingUserInteractionDelay)
                context.stopService(this@run)
            }
        }
    }

}

class DataPointCollectionForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        val dataPointCollectionChannelId = "DataPointCollection"
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(
                NotificationChannel(
                    dataPointCollectionChannelId,
                    "Data Point Collection",
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        val notification = Notification.Builder(this, dataPointCollectionChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Data Point Collection")
            .setContentText("Collecting data point from device sensors in background...")
            .build()
        val dataPointCollectionNotificationId = 1
        (getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(dataPointCollectionNotificationId)
        startForeground(dataPointCollectionNotificationId, notification)
    }

    override fun onBind(intent: Intent?): IBinder? =
        null
}