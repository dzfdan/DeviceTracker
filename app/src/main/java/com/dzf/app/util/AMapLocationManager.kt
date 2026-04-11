package com.dzf.app.util

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log

class AMapLocationManager(
    private val context: Context,
    private val onLocationChanged: (Location) -> Unit,
    private val onError: (Int, String) -> Unit = { _, _ -> }
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val mainLooper = Looper.getMainLooper()
    private var continuousListener: LocationListener? = null

    init {
        Log.d(TAG, "Initializing system location manager")
        checkLocationServices()
    }

    private fun checkLocationServices() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        Log.d(TAG, "GPS enabled: $gpsEnabled, Network enabled: $networkEnabled")

        if (!gpsEnabled && !networkEnabled) {
            Log.w(TAG, "No location providers enabled! Please enable GPS in Settings")
        }
    }

    fun startLocation() {
        val provider = getBestProvider() ?: run {
            onError(ERROR_NO_PROVIDER, "No enabled location provider")
            return
        }

        val listener = LocationListener { location ->
            if (isValidLocation(location)) {
                Log.d(TAG, "onLocationChanged: lat=${location.latitude}, lng=${location.longitude}, accuracy=${location.accuracy}, provider=${location.provider}")
                onLocationChanged(location)
            } else {
                onError(ERROR_INVALID_LOCATION, "Invalid latitude or longitude from ${location.provider}")
            }
        }

        continuousListener = listener
        try {
            locationManager.requestLocationUpdates(provider, 10_000L, 0f, listener, mainLooper)
            getLastKnownLocation()?.let { onLocationChanged(it) }
            Log.d(TAG, "Started system location updates with provider=$provider")
        } catch (se: SecurityException) {
            Log.e(TAG, "Missing location permission", se)
            onError(ERROR_PERMISSION, "Missing location permission")
        }
    }

    fun enableBackgroundLocation(notificationId: Int, notification: Notification) {
        Log.d(TAG, "System provider does not require SDK background hook")
    }

    fun disableBackgroundLocation() {
        Log.d(TAG, "System provider background hook disabled")
    }

    fun stopLocation() {
        continuousListener?.let {
            locationManager.removeUpdates(it)
            continuousListener = null
        }
    }

    fun restartLocation() {
        stopLocation()
        startLocation()
    }

    fun getLastKnownLocation(): Location? {
        return try {
            val candidates = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            ).mapNotNull { provider ->
                if (locationManager.isProviderEnabled(provider)) locationManager.getLastKnownLocation(provider) else null
            }
            candidates.maxByOrNull { it.time }
        } catch (se: SecurityException) {
            onError(ERROR_PERMISSION, "Missing location permission")
            null
        }
    }

    @SuppressLint("CheckResult")
    fun requestOnceLocation(onResult: (Location?) -> Unit) {
        val provider = getBestProvider() ?: run {
            onError(ERROR_NO_PROVIDER, "No enabled location provider")
            onResult(null)
            return
        }

        val timeoutHandler = Handler(mainLooper)
        var completed = false

        val timeoutRunnable = Runnable {
            if (!completed) {
                completed = true
                onError(ERROR_TIMEOUT, "Single location request timed out")
                onResult(null)
            }
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                timeoutHandler.postDelayed(timeoutRunnable, 20_000L)
                locationManager.getCurrentLocation(provider, null, context.mainExecutor) { location ->
                    if (!completed) {
                        completed = true
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        if (location != null && isValidLocation(location)) {
                            onResult(location)
                        } else {
                            onError(ERROR_INVALID_LOCATION, "Invalid single location result")
                            onResult(null)
                        }
                    }
                }
                return
            }

            val oneShotListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (!completed) {
                        completed = true
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        locationManager.removeUpdates(this)
                        if (isValidLocation(location)) {
                            onResult(location)
                        } else {
                            onError(ERROR_INVALID_LOCATION, "Invalid single location result")
                            onResult(null)
                        }
                    }
                }
            }

            timeoutHandler.postDelayed(timeoutRunnable, 20_000L)
            locationManager.requestLocationUpdates(provider, 0L, 0f, oneShotListener, mainLooper)
        } catch (se: SecurityException) {
            onError(ERROR_PERMISSION, "Missing location permission")
            onResult(null)
        }
    }

    fun destroy() {
        disableBackgroundLocation()
        stopLocation()
    }

    private fun getBestProvider(): String? {
        return try {
            val criteria = Criteria().apply {
                accuracy = Criteria.ACCURACY_FINE
                isAltitudeRequired = false
                isBearingRequired = false
                isSpeedRequired = false
                powerRequirement = Criteria.POWER_LOW
            }

            locationManager.getBestProvider(criteria, true)
                ?: when {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> null
                }
        } catch (e: Exception) {
            null
        }
    }

    private fun isValidLocation(location: Location): Boolean {
        return location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0
    }

    companion object {
        private const val TAG = "AMapLocationManager"
        private const val ERROR_NO_PROVIDER = 1
        private const val ERROR_PERMISSION = 2
        private const val ERROR_TIMEOUT = 3
        private const val ERROR_INVALID_LOCATION = 4
    }
}
