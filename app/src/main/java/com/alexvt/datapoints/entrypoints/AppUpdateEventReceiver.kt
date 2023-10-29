package com.alexvt.datapoints.entrypoints

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.datapoints.datacollection.scheduleSingleCollectionAtNextMinuteStart

class AppUpdateEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> scheduleSingleCollectionAtNextMinuteStart(context)
        }
    }
}