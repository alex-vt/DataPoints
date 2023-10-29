package com.alexvt.datapoints.datacollection

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.alexvt.datapoints.entrypoints.DataPointCollectionTimeEventReceiver
import kotlin.math.ceil

@SuppressLint("ScheduleExactAlarm") // declared in manifest; critical for data quality
fun scheduleSingleCollectionAtNextMinuteStart(context: Context) {
    val actionIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, DataPointCollectionTimeEventReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_ONE_SHOT,
    )
    val nextMinuteStartMillis = ceil(System.currentTimeMillis() / 60_000.0).toLong() * 60_000
    (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).run {
        cancel(actionIntent)
        setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMinuteStartMillis, actionIntent)
    }
}