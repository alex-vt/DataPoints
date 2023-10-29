package com.alexvt.datapoints.entrypoints

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import com.alexvt.datapoints.BuildConfig
import com.alexvt.datapoints.datacollection.getDataPointsForDate
import com.alexvt.datapoints.datacollection.scheduleSingleCollectionAtNextMinuteStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class UiScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleSingleCollectionAtNextMinuteStart(this)

        requestPermissions(
            listOfNotNull(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    null
                },
            )
        ) {
            requestAllFileAccess()
        }

        setContent {
            Column {
                val coroutineScope = rememberCoroutineScope()
                val listState = rememberLazyListState()
                val dataPointLines = remember {
                    getDataPointsForToday().toMutableStateList().also { initialDataPoints ->
                        scrollToEnd(coroutineScope, listState, data = initialDataPoints)
                    }
                }
                TextButton(
                    onClick = {
                        dataPointLines.clear()
                        dataPointLines.addAll(getDataPointsForToday())
                        scrollToEnd(coroutineScope, listState, data = dataPointLines)
                    }
                ) {
                    Text(text = "Reload today's data points")
                }
                LazyColumn(state = listState) {
                    items(dataPointLines.size) { dataPointIndex ->
                        Text(text = dataPointLines[dataPointIndex])
                    }
                }
            }
        }

        closeOnBackPressed()
    }

    private fun getDataPointsForToday(): List<String> =
        if (Environment.isExternalStorageManager()) {
            getDataPointsForDate(System.currentTimeMillis(), this)
        } else {
            listOf("No file access")
        }

    private fun scrollToEnd(
        coroutineScope: CoroutineScope,
        listState: LazyListState,
        data: List<Any>,
    ) {
        coroutineScope.launch {
            listState.animateScrollToItem(index = data.size - 1)
        }
    }

    /**
     * Batch permission requesting, sequentially, accumulating results.
     * Unlike when using ActivityResultContracts.RequestMultiplePermissions,
     * will not "refuse" to request the whole set if some need to be separate,
     * such as ACCESS_BACKGROUND_LOCATION.
     * Call this before onStart().
     */
    private fun requestPermissions(permissions: List<String>, onResults: (List<Boolean>) -> Unit) {
        if (permissions.isEmpty()) return onResults(emptyList())
        var launchers: List<ActivityResultLauncher<String>> = emptyList()
        val results: MutableList<Boolean> = mutableListOf()
        launchers = List(permissions.size) { index ->
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                results += result
                if (index < permissions.size - 1) {
                    launchers[index + 1].launch(permissions[index + 1])
                } else {
                    onResults(results)
                }
            }
        }
        launchers.first().launch(permissions.first())
    }

    private fun requestAllFileAccess() {
        if (!Environment.isExternalStorageManager()) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:${BuildConfig.APPLICATION_ID}"),
                )
            )
        }
    }

    private fun closeOnBackPressed() {
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }
}
