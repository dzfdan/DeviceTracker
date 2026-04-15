package com.dzf.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dzf.app.BuildConfig
import com.dzf.app.R
import com.dzf.app.model.DeviceLocation
import com.dzf.app.util.AMapLocationManager
import com.dzf.app.util.AppLifecycleState
import com.dzf.app.util.CoordinateTransform
import com.dzf.app.util.DeviceInfoHelper
import kotlinx.coroutines.*

class LocationTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var locationManager: AMapLocationManager
    private val cloudBase = CloudBaseRepository.getInstance()
    private var backgroundTickJob: Job? = null
    private var lastBackgroundRequestTime: Long = 0

    private lateinit var deviceId: String
    private lateinit var deviceName: String

    private var lastUploadTime: Long = 0
    private var lastUploadedLatitude: Double? = null
    private var lastUploadedLongitude: Double? = null
    private var pendingLatitude: Double? = null
    private var pendingLongitude: Double? = null
    private var pendingCount: Int = 0
    private var lastErrorNotifyTime: Long = 0
    private val foregroundUploadIntervalMs: Long = 10_000
    private val backgroundUploadIntervalMs: Long =
        BuildConfig.BACKGROUND_UPLOAD_INTERVAL_SECONDS.toLong().coerceAtLeast(1L) * 1000
    private val minUploadDistanceMeters: Float =
        BuildConfig.MIN_UPLOAD_DISTANCE_METERS.coerceAtLeast(0f)
    private val maxNoUploadIntervalMs: Long =
        (BuildConfig.BACKGROUND_UPLOAD_INTERVAL_SECONDS.toLong().coerceAtLeast(1L) * 3_000)
            .coerceAtLeast(15 * 60_000L)
    private val confirmRadiusMeters = 60f
    private val requiredConfirmCountBase = 2
    private val errorNotifyCooldownMs = 60_000L
    private val maxFallbackLocationAgeMs = 15 * 60_000L

    override fun onCreate() {
        super.onCreate()
        setRunning(true)
        createNotificationChannel()
        ensureForegroundMode()

        deviceId = DeviceInfoHelper.getDeviceId(this)
        deviceName = DeviceInfoHelper.getDeviceName(this)

        locationManager = AMapLocationManager(
            context = this,
            onLocationChanged = { location -> onLocationReceived(location) },
            onError = { code, info ->
                Log.e(TAG, "Location error: code=$code, info=$info")
                notifyLocationFailure(code, info)
                if (code == 3) {
                    tryUploadLastKnownLocation(code, info)
                }
                if (code == 12 || code == 11 || code == 1 || code == 8) {
                    locationManager.restartLocation()
                }
                Toast.makeText(this, "Location failed: $info", Toast.LENGTH_LONG).show()
            }
        )

        locationManager.enableBackgroundLocation(NOTIFICATION_ID, buildNotification())

        locationManager.startLocation()
        startBackgroundTicker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForegroundMode()
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        setRunning(false)
        locationManager.stopLocation()
        locationManager.destroy()
        backgroundTickJob?.cancel()
        serviceScope.cancel()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartIntent = Intent(applicationContext, LocationTrackingService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext,
            2001,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 3000,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                android.app.AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + 3000,
                pendingIntent
            )
        }
    }

    private fun ensureForegroundMode() {
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun onLocationReceived(location: Location) {
        Log.d(TAG, "Location received: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}")
        val (gcjLat, gcjLng) = CoordinateTransform.wgs84ToGcj02(location.latitude, location.longitude)
        val now = System.currentTimeMillis()
        val elapsedSinceLastUpload = now - lastUploadTime

        val uploadInterval = if (AppLifecycleState.isInForeground()) {
            foregroundUploadIntervalMs
        } else {
            backgroundUploadIntervalMs
        }

        if (elapsedSinceLastUpload < uploadInterval) {
            return
        }

        val prevLat = lastUploadedLatitude
        val prevLng = lastUploadedLongitude
        if (prevLat != null && prevLng != null) {
            val distance = FloatArray(1)
            Location.distanceBetween(prevLat, prevLng, gcjLat, gcjLng, distance)
            val shouldKeepAliveUpload = elapsedSinceLastUpload >= maxNoUploadIntervalMs
            if (distance[0] < minUploadDistanceMeters) {
                if (!shouldKeepAliveUpload) {
                    Log.d(TAG, "Skip upload: moved ${distance[0]}m (< ${minUploadDistanceMeters}m)")
                    pendingLatitude = null
                    pendingLongitude = null
                    pendingCount = 0
                    return
                }

                Log.d(
                    TAG,
                    "Keepalive upload: moved ${distance[0]}m, but no upload for ${elapsedSinceLastUpload}ms"
                )
            }

            val requiredConfirmCount = if (AppLifecycleState.isInForeground()) {
                requiredConfirmCountBase
            } else {
                1
            }

            if (requiredConfirmCount > 1) {
                val candLat = pendingLatitude
                val candLng = pendingLongitude
                if (candLat == null || candLng == null) {
                    pendingLatitude = gcjLat
                    pendingLongitude = gcjLng
                    pendingCount = 1
                    Log.d(TAG, "Pending new position: waiting for confirmation")
                    return
                }

                val confirmDistance = FloatArray(1)
                Location.distanceBetween(candLat, candLng, gcjLat, gcjLng, confirmDistance)
                if (confirmDistance[0] <= confirmRadiusMeters) {
                    pendingCount += 1
                } else {
                    pendingLatitude = gcjLat
                    pendingLongitude = gcjLng
                    pendingCount = 1
                    Log.d(TAG, "Pending position changed: reset confirmation")
                    return
                }

                if (pendingCount < requiredConfirmCount) {
                    Log.d(TAG, "Pending new position: confirm ${pendingCount}/$requiredConfirmCount")
                    return
                }
            }
        }

        val deviceLocation = DeviceLocation(
            deviceId = deviceId,
            deviceName = deviceName,
            latitude = gcjLat,
            longitude = gcjLng,
            timestamp = now,
            isOnline = true
        )

        serviceScope.launch {
            val result = cloudBase.upsertDeviceLocation(deviceLocation)
            result.fold(
                onSuccess = {
                    lastUploadTime = now
                    lastUploadedLatitude = gcjLat
                    lastUploadedLongitude = gcjLng
                    pendingLatitude = null
                    pendingLongitude = null
                    pendingCount = 0
                    Log.d(TAG, "Location uploaded successfully")
                },
                onFailure = { Log.e(TAG, "Failed to upload location: ${it.message}") }
            )
        }
    }

    private fun startBackgroundTicker() {
        backgroundTickJob?.cancel()
        backgroundTickJob = serviceScope.launch {
            while (isActive) {
                try {
                    if (!AppLifecycleState.isInForeground()) {
                        val now = System.currentTimeMillis()
                        val elapsed = now - lastBackgroundRequestTime
                        if (elapsed >= backgroundUploadIntervalMs) {
                            lastBackgroundRequestTime = now
                            Log.d(
                                TAG,
                                "Background one-shot request started, elapsed=${elapsed}ms, interval=${backgroundUploadIntervalMs}ms"
                            )
                            locationManager.requestOnceLocation { oneShot ->
                                if (oneShot != null) {
                                    onLocationReceived(oneShot)
                                } else {
                                    Log.w(
                                        TAG,
                                        "Background one-shot location returned null. " +
                                        "Failure reason should already be logged by location manager onError."
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Background ticker error", e)
                }
                delay(10_000)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.location_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.location_service_channel_description)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                getString(R.string.location_alert_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.location_alert_channel_description)
            }
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun notifyLocationFailure(code: Int, info: String) {
        val now = System.currentTimeMillis()
        Log.e(TAG, "notifyLocationFailure: code=$code, info=$info")
        if (now - lastErrorNotifyTime < errorNotifyCooldownMs) {
            Log.w(TAG, "Location failure notification suppressed by cooldown")
            return
        }
        lastErrorNotifyTime = now

        val message = getString(R.string.location_failed_text, code, info)
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(getString(R.string.location_failed_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(LOCATION_ERROR_NOTIFICATION_ID, notification)
        Log.e(TAG, "Location failure notification posted")
    }

    private fun tryUploadLastKnownLocation(code: Int, info: String) {
        val fallback = locationManager.getLastKnownLocation()
        if (fallback == null) {
            Log.w(TAG, "Timeout fallback skipped: no last known location. code=$code, info=$info")
            return
        }

        val now = System.currentTimeMillis()
        val locationAgeMs = now - fallback.time
        if (locationAgeMs > maxFallbackLocationAgeMs) {
            Log.w(
                TAG,
                "Timeout fallback skipped: last known location too old (${locationAgeMs}ms). code=$code, info=$info"
            )
            return
        }

        Log.w(
            TAG,
            "Timeout fallback using last known location: age=${locationAgeMs}ms, provider=${fallback.provider}"
        )
        onLocationReceived(fallback)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.location_service_title))
            .setContentText(getString(R.string.location_service_text))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "LocationTrackingService"
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val ALERT_CHANNEL_ID = "location_alert_channel"
        private const val NOTIFICATION_ID = 1001
        private const val LOCATION_ERROR_NOTIFICATION_ID = 2002

        private var isRunning = false
        fun isServiceRunning(): Boolean = isRunning

        fun setRunning(running: Boolean) {
            isRunning = running
        }
    }
}
