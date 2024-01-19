package com.alexvt.datapoints.datacollection

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.BatteryManager
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.provider.Settings.Global.DEVICE_NAME
import com.alexvt.datapoints.BuildConfig
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.io.path.Path
import kotlin.math.abs
import kotlin.math.sqrt


// Data structure config

private val dataPointsHeader =
    listOf(
        "Date",
        "Time",
        "Timezone",
        "ScreenState",
        "BatteryPercent",
        "UnlockState",
        "Latitude",
        "Longitude",
        "Pressure",
        "Bearing",
        "Pitch",
        "Tilt",
        "Acceleration",
        "Temperature",
        "Lux",
        "DataPointDuration",
    )

private suspend fun getDataPointsRow(timestamp: Long, context: Context) =
    coroutineScope {
        val locationDeferred = async { getLocation(context) }
        listOf(
            async { getDate(timestamp) },
            async { getTime(timestamp) },
            async { getTimezone(timestamp) },
            async { getScreenState(context) },
            async { getBatteryPercent(context) },
            async { getUnlockState(context) },
            async { getLatitude(locationDeferred.await()) },
            async { getLongitude(locationDeferred.await()) },
            async { getPressure(context) },
            async { getBearing(context) },
            async { getPitch(context) },
            async { getTilt(context) },
            async { getAcceleration(context) },
            async { getTemperature() },
            async { getLux(context) },
        ).awaitAll() + // all readings in parallel...
                getDataPointDuration(timestamp) // ...except duration: after all
    }


// Data point collection

fun getDataPointsForDate(timestamp: Long, context: Context): List<String> =
    getTodayDataPointsFile(timestamp, context).readLines()

suspend fun recordDataPoint(timestamp: Long, context: Context) {
    getTodayDataPointsFile(timestamp, context)
        .appendText(
            getDataPointsRow(timestamp, context)
                .joinToString(prefix = "\n", separator = ",")
        )
}

private fun getTodayDataPointsFile(timestamp: Long, context: Context): File {
    val storageRootPath = Environment.getExternalStorageDirectory().path
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(timestamp)
    val deviceName = Settings.Global.getString(context.contentResolver, DEVICE_NAME)
    val extension = "csv"

    return Path(storageRootPath, BuildConfig.CSV_RELATIVE_PATH, "${date}_${deviceName}.$extension")
        .toFile().apply {
            if (!exists()) {
                parentFile?.mkdirs()
                writeText(dataPointsHeader.joinToString(separator = ","))
            }
        }
}


// Individual data readings

private fun getDate(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(timestamp)

private fun getTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.US).format(timestamp)

private fun getTimezone(timestamp: Long): String =
    SimpleDateFormat("Z", Locale.US).format(timestamp)

private fun getScreenState(context: Context): String =
    (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
        .isInteractive
        .run { if (this) "1" else "0" }

private fun getBatteryPercent(context: Context): String =
    (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        .toString()

private fun getUnlockState(context: Context): String =
    (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
        .isKeyguardLocked
        .run { if (this) "0" else "1" }

private fun getLatitude(
    location: Location?,
    title: String = "Latitude",
    readingMissingSuffix: String = "Missing",
): String =
    location?.latitude?.toFloat()?.toString() ?: "$title$readingMissingSuffix"

private fun getLongitude(
    location: Location?,
    title: String = "Longitude",
    readingMissingSuffix: String = "Missing",
): String =
    location?.longitude?.toFloat()?.toString() ?: "$title$readingMissingSuffix"

private suspend fun getPressure(context: Context): String =
    getSensorReadout(context, type = Sensor.TYPE_PRESSURE, title = "Pressure") {
        get(0).toString()
    }

private suspend fun getBearing(context: Context): String =
    getSensorReadout(context, type = Sensor.TYPE_ORIENTATION, title = "Bearing") {
        get(0).toString()
    }

private suspend fun getPitch(context: Context): String =
    getSensorReadout(context, type = Sensor.TYPE_ORIENTATION, title = "Pitch") {
        get(1).unaryMinus().run { // enforce pitch range to -90..90 degrees
            when {
                this > 90 -> 180 - this
                this < -90 -> -180 - this
                else -> this
            }
        }.toString()
    }

private suspend fun getTilt(context: Context): String =
    getSensorReadout(context, type = Sensor.TYPE_ORIENTATION, title = "Tilt") {
        get(2).unaryMinus().toString()
    }

private suspend fun getAcceleration(context: Context): String =
    getSensorReadout(context, type = Sensor.TYPE_ACCELEROMETER, title = "Acceleration") {
        abs(sqrt(get(0) * get(0) + get(1) * get(1) + get(2) * get(2)) - SensorManager.GRAVITY_EARTH)
            .toString()
    }

private fun getTemperature(
    title: String = "Temperature",
    readingNoSensorSuffix: String = "NoSensor",
): String {
    val thermalSensorPath = "/sys/devices/virtual/thermal/thermal_zone0/temp"
    return File(thermalSensorPath)
        .readLines(Charset.defaultCharset()).firstOrNull()?.run { toInt() / 1000 }?.toString()
        ?: "$title$readingNoSensorSuffix"
}

private suspend fun getLux(context: Context): String =
    getSensorReadout(context, type = Sensor.TYPE_LIGHT, title = "Lux") {
        get(0).toString()
    }

private fun getDataPointDuration(timestamp: Long): String =
    (System.currentTimeMillis() - timestamp).toString()


// Helpers

@SuppressLint("MissingPermission") // handled in UI; best effort - any or empty data
private suspend fun getLocation(context: Context): Location? =
    try {
        val locationDataRetrievalTimeoutMillis = 25_000L
        withTimeout(locationDataRetrievalTimeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val cancellationTokenSource = CancellationTokenSource()
                LocationServices.getFusedLocationProviderClient(context)
                    .getCurrentLocation(
                        CurrentLocationRequest.Builder()
                            .setMaxUpdateAgeMillis(0)
                            .setDurationMillis(locationDataRetrievalTimeoutMillis)
                            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                            .build(),
                        cancellationTokenSource.token,
                    )
                    .addOnSuccessListener(continuation::resume)
                    .addOnFailureListener { _ ->
                        continuation.resume(null)
                    }
            }
        }
    } catch (timeoutException: TimeoutCancellationException) {
        null
    }

private suspend fun getSensorReadout(
    context: Context,
    type: Int,
    title: String,
    noSensorSuffix: String = "NoSensor",
    readingMissingSuffix: String = "Missing",
    readingTimeoutSuffix: String = "Timeout",
    valueExtractor: FloatArray.() -> String,
): String =
    try {
        val sensorDataRetrievalTimeoutMillis = 2_000L
        withTimeout(sensorDataRetrievalTimeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val sensorManager =
                    context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                val sensor = sensorManager.getDefaultSensor(type)
                if (sensor == null) {
                    continuation.resume("$title$noSensorSuffix")
                    return@suspendCancellableCoroutine
                }
                val listener = object : SensorEventListener {
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

                    override fun onSensorChanged(event: SensorEvent?) {
                        sensorManager.unregisterListener(this)
                        continuation.resume(event?.values?.run { valueExtractor() }
                            ?: "$title$readingMissingSuffix")
                    }
                }
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
            }

        }
    } catch (timeoutException: TimeoutCancellationException) {
        "$title$readingTimeoutSuffix"
    }
