package com.alexvt.datapoints.entrypoints

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.alexvt.datapoints.datacollection.scheduleSingleCollectionAtNextMinuteStart

class SystemBootEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> scheduleSingleCollectionAtNextMinuteStart(context)
        }
    }
}