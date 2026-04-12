package com.dzf.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.dzf.app.R
import com.dzf.app.databinding.ActivityMainBinding
import com.dzf.app.model.DeviceLocation
import com.dzf.app.service.CloudBaseRepository
import com.dzf.app.service.LocationTrackingService
import com.dzf.app.util.AMapLocationManager
import com.dzf.app.util.DeviceInfoHelper
import com.dzf.app.util.PermissionHelper
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var aMap: AMap
    private val cloudBase = CloudBaseRepository.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = MainScope()
    private var locationManager: AMapLocationManager? = null

    private val markers = mutableMapOf<String, Marker>()
    private lateinit var deviceId: String
    private var isMapLoaded = false
    private var hasStartedTracking = false

    private val trackPoints = mutableListOf<LatLng>()
    private var trackPolyline: com.amap.api.maps.model.Polyline? = null
    private var isTracking = false

    private val refreshInterval: Long = 15000
    private val mapLoadTimeoutMs: Long = 8_000

    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadDeviceLocations()
            handler.postDelayed(this, refreshInterval)
        }
    }

    private val mapLoadTimeoutRunnable = Runnable {
        if (!isMapLoaded) {
            Log.w(TAG, "Map load timeout. Please verify AMAP_KEY and network.")
            Toast.makeText(this, getString(R.string.map_load_timeout_hint), Toast.LENGTH_LONG).show()
        }
    }

    private val requestLocationPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantMap ->
            val allGranted = grantMap.values.all { it }
            if (allGranted) {
                if (!PermissionHelper.hasBackgroundLocationPermission(this) &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ) {
                    showBackgroundPermissionDialog()
                } else {
                    maybeStartTracking()
                }
            } else {
                Toast.makeText(this, "Location permission is required for this app to work", Toast.LENGTH_LONG).show()
            }
        }

    private val requestBackgroundPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Toast.makeText(
                    this,
                    "Background location denied. Upload may pause in background.",
                    Toast.LENGTH_LONG
                ).show()
            }
            maybeStartTracking()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceId = DeviceInfoHelper.getDeviceId(this)

        binding.mapView.onCreate(savedInstanceState)
        aMap = binding.mapView.map
        aMap.uiSettings.isZoomControlsEnabled = false
        aMap.setOnMarkerClickListener { marker ->
            showMarkerInfo(marker)
            true
        }

        binding.myLocationFab.setOnClickListener {
            moveToMyLocation()
        }

        binding.trackFab.visibility = View.GONE
        binding.trackCard.visibility = View.GONE

        aMap.setOnMapLoadedListener {
            isMapLoaded = true
            handler.removeCallbacks(mapLoadTimeoutRunnable)
            Log.d(TAG, "Map loaded successfully")
            maybeStartTracking()
        }

        binding.deviceCountCard.setOnClickListener {
            startActivity(Intent(this, DeviceListActivity::class.java))
        }

        checkPermissionsAndStart()
        handler.postDelayed(mapLoadTimeoutRunnable, mapLoadTimeoutMs)
    }

    private fun maybeStartTracking() {
        if (hasStartedTracking || !isMapLoaded) return
        if (!PermissionHelper.hasLocationPermission(this)) return

        hasStartedTracking = true
        startLocationUpdates()
        startLocationService()
        loadDeviceLocations()
        handler.postDelayed(refreshRunnable, refreshInterval)
    }

    private fun checkPermissionsAndStart() {
        if (!PermissionHelper.hasLocationPermission(this)) {
            requestLocationPermissionsLauncher.launch(PermissionHelper.requiredPermissions())
        } else if (!PermissionHelper.hasBackgroundLocationPermission(this)) {
            showBackgroundPermissionDialog()
        } else {
            maybeStartTracking()
        }
    }

    private fun showBackgroundPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Background Location")
            .setMessage("To keep your location updated when the app is in the background, please enable 'Allow all the time' location permission in the next screen.")
            .setPositiveButton("Grant") { _, _ ->
                requestBackgroundPermissionLauncher.launch(PermissionHelper.backgroundLocationPermission())
            }
            .setNegativeButton("Skip") { _, _ ->
                maybeStartTracking()
            }
            .show()
    }

    private fun startLocationUpdates() {
        locationManager?.destroy()
        locationManager = null

        locationManager = AMapLocationManager(
            context = this,
            onLocationChanged = { location ->
                runOnUiThread {
                    updateMyLocationOnMap(location)
                }
            },
            onError = { code, info ->
                Log.e(TAG, "Location error: code=$code, info=$info")
            }
        )
        locationManager?.startLocation()
    }

    private var myLocationMarker: Marker? = null
    private var displayedMyLatLng: LatLng? = null
    private var pendingMyLatLng: LatLng? = null
    private var pendingMyCount: Int = 0
    private val myJumpThresholdMeters = 100f
    private val myConfirmRadiusMeters = 60f
    private val myRequiredConfirmCount = 2

    private fun updateMyLocationOnMap(location: Location) {
        val rawLatLng = LatLng(location.latitude, location.longitude)
        val latLng = stabilizeMyLocation(rawLatLng) ?: return
        Log.d(TAG, "Updating my location on map: lat=${latLng.latitude}, lng=${latLng.longitude}")

        if (isTracking) {
            trackPoints.add(latLng)
            updateTrackPolyline()
            updateTrackInfo()
        }

        if (myLocationMarker == null) {
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title(DeviceInfoHelper.getDeviceName(this))
                .snippet(getString(R.string.this_device))
                .icon(createMyLocationBitmap())
                .draggable(false)
            myLocationMarker = aMap.addMarker(markerOptions)
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        } else {
            myLocationMarker?.setPosition(latLng)
        }
    }

    private fun stabilizeMyLocation(newLatLng: LatLng): LatLng? {
        val current = displayedMyLatLng
        if (current == null) {
            displayedMyLatLng = newLatLng
            return newLatLng
        }

        val jumpDistance = FloatArray(1)
        Location.distanceBetween(
            current.latitude,
            current.longitude,
            newLatLng.latitude,
            newLatLng.longitude,
            jumpDistance
        )
        if (jumpDistance[0] < myJumpThresholdMeters) {
            pendingMyLatLng = null
            pendingMyCount = 0
            displayedMyLatLng = newLatLng
            return newLatLng
        }

        val pending = pendingMyLatLng
        if (pending == null) {
            pendingMyLatLng = newLatLng
            pendingMyCount = 1
            return null
        }

        val confirmDistance = FloatArray(1)
        Location.distanceBetween(
            pending.latitude,
            pending.longitude,
            newLatLng.latitude,
            newLatLng.longitude,
            confirmDistance
        )
        if (confirmDistance[0] <= myConfirmRadiusMeters) {
            pendingMyCount += 1
        } else {
            pendingMyLatLng = newLatLng
            pendingMyCount = 1
            return null
        }

        if (pendingMyCount < myRequiredConfirmCount) {
            return null
        }

        displayedMyLatLng = newLatLng
        pendingMyLatLng = null
        pendingMyCount = 0
        return newLatLng
    }

    private fun toggleTracking() {
        if (isTracking) {
            stopTracking()
        } else {
            startTracking()
        }
    }

    private fun startTracking() {
        isTracking = true
        trackPoints.clear()
        trackPolyline?.remove()
        trackPolyline = null
        binding.trackFab.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        binding.trackCard.visibility = View.VISIBLE
        Toast.makeText(this, getString(R.string.tracking_started), Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Tracking started")
    }

    private fun stopTracking() {
        isTracking = false
        binding.trackFab.setImageResource(android.R.drawable.ic_menu_directions)
        Toast.makeText(this, getString(R.string.tracking_stopped), Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Tracking stopped. Total points: ${trackPoints.size}")
    }

    private fun updateTrackPolyline() {
        if (trackPoints.size < 2) return

        trackPolyline?.remove()

        trackPolyline = aMap.addPolyline(
            PolylineOptions()
                .addAll(trackPoints)
                .color(Color.parseColor("#1976D2"))
                .width(10f)
                .geodesic(true)
        )
    }

    private fun updateTrackInfo() {
        val distance = calculateTotalDistance()
        binding.trackInfoText.text = getString(R.string.track_info, trackPoints.size, distance)
    }

    private fun calculateTotalDistance(): Double {
        var totalDistance = 0.0
        for (i in 1 until trackPoints.size) {
            totalDistance += calculateDistance(
                trackPoints[i - 1].latitude, trackPoints[i - 1].longitude,
                trackPoints[i].latitude, trackPoints[i].longitude
            )
        }
        return totalDistance / 1000
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    private fun createMyLocationBitmap(): com.amap.api.maps.model.BitmapDescriptor {
        val name = DeviceInfoHelper.getDeviceName(this)
        val color = getColorCompat(R.color.marker_current)
        return createLabeledMarkerIcon(name = name, color = color, highlight = true)
    }

    private fun startLocationService() {
        if (!LocationTrackingService.isServiceRunning()) {
            val intent = Intent(this, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            LocationTrackingService.setRunning(true)
        }
    }

    private fun loadDeviceLocations() {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                cloudBase.getAllDeviceLocations()
            }

            result.fold(
                onSuccess = { devices ->
                    updateMarkers(devices)
                    runOnUiThread {
                        binding.deviceCountText.text = getString(R.string.device_count, devices.size)
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to load devices: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun updateMarkers(devices: List<DeviceLocation>) {
        val otherDevices = devices.filter { it.deviceId != deviceId }
        val currentDeviceIds = otherDevices.map { it.deviceId }.toSet()

        val toRemove = markers.keys - currentDeviceIds
        toRemove.forEach { id ->
            markers[id]?.remove()
            markers.remove(id)
        }

        otherDevices.forEach { device ->
            val latLng = LatLng(device.latitude, device.longitude)
            val existingMarker = markers[device.deviceId]

            if (existingMarker != null) {
                existingMarker.setPosition(latLng)
                existingMarker.setTitle(device.deviceName)
                existingMarker.setSnippet(buildMarkerSnippet(device))
                updateMarkerIcon(existingMarker, device)
            } else {
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .title(device.deviceName)
                    .snippet(buildMarkerSnippet(device))
                    .icon(createMarkerBitmap(device))
                val marker = aMap.addMarker(markerOptions)
                if (marker != null) {
                    markers[device.deviceId] = marker
                }
            }
        }
    }

    private fun buildMarkerSnippet(device: DeviceLocation): String {
        val status = if (device.isOnline) getString(R.string.online) else getString(R.string.offline)
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val time = timeFormat.format(Date(device.timestamp))
        return "$status | $time"
    }

    private fun updateMarkerIcon(marker: Marker, device: DeviceLocation) {
        marker.setIcon(createMarkerBitmap(device))
    }

    private fun createMarkerBitmap(device: DeviceLocation): com.amap.api.maps.model.BitmapDescriptor {
        val color = when {
            device.deviceId == deviceId -> getColorCompat(R.color.marker_current)
            device.isOnline -> getColorCompat(R.color.marker_other)
            else -> getColorCompat(R.color.marker_offline)
        }
        val name = device.deviceName.ifBlank { "Unknown" }
        return createLabeledMarkerIcon(
            name = name,
            color = color,
            highlight = device.deviceId == deviceId
        )
    }

    private fun createLabeledMarkerIcon(
        name: String,
        color: Int,
        highlight: Boolean
    ): com.amap.api.maps.model.BitmapDescriptor {
        val width = 200
        val height = 230
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = width / 2f
        val circleCy = 82f
        val circleRadius = 42f

        if (highlight) {
            val glowPaint = Paint().apply {
                this.color = color
                alpha = 70
                isAntiAlias = true
            }
            canvas.drawCircle(cx, circleCy, circleRadius + 24f, glowPaint)
        }

        val pinPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
        }
        canvas.drawCircle(cx, circleCy, circleRadius, pinPaint)

        val pinTail = Path().apply {
            moveTo(cx, circleCy + circleRadius - 2f)
            lineTo(cx - 18f, circleCy + circleRadius + 40f)
            lineTo(cx + 18f, circleCy + circleRadius + 40f)
            close()
        }
        canvas.drawPath(pinTail, pinPaint)

        val pinBorder = Paint().apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }
        canvas.drawCircle(cx, circleCy, circleRadius, pinBorder)
        canvas.drawPath(pinTail, pinBorder)

        val centerDot = Paint().apply {
            this.color = Color.WHITE
            isAntiAlias = true
        }
        canvas.drawCircle(cx, circleCy, 12f, centerDot)

        val labelRect = RectF(26f, 166f, width - 26f, 214f)
        val labelBg = Paint().apply {
            this.color = Color.parseColor("#F7FAFC")
            isAntiAlias = true
        }
        canvas.drawRoundRect(labelRect, 24f, 24f, labelBg)

        val labelBorder = Paint().apply {
            this.color = Color.parseColor("#CFD8DC")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            isAntiAlias = true
        }
        canvas.drawRoundRect(labelRect, 24f, 24f, labelBorder)

        val textPaint = Paint().apply {
            this.color = Color.parseColor("#102027")
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = 25f
        }
        val displayName = if (name.length > 12) name.take(12) + "..." else name
        canvas.drawText(displayName, cx, 198f, textPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun showMarkerInfo(marker: Marker) {
        val view = layoutInflater.inflate(R.layout.info_window, null)
        val nameText = view.findViewById<TextView>(R.id.deviceNameText)
        val statusText = view.findViewById<TextView>(R.id.deviceStatusText)
        val timeText = view.findViewById<TextView>(R.id.deviceTimeText)

        nameText.text = marker.title

        val snippet = marker.snippet ?: ""
        val parts = snippet.split(" | ")
        if (parts.size >= 2) {
            statusText.text = parts[0]
            timeText.text = getString(R.string.last_seen, parts[1])
        }

        if (marker.title == DeviceInfoHelper.getDeviceName(this)) {
            nameText.text = "${marker.title} (${getString(R.string.this_device)})"
        }

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun moveToMyLocation() {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                cloudBase.getAllDeviceLocations()
            }
            result.fold(
                onSuccess = { devices ->
                    val myDevice = devices.find { it.deviceId == deviceId }
                    if (myDevice != null) {
                        val latLng = LatLng(myDevice.latitude, myDevice.longitude)
                        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Waiting for location...", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onFailure = {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Failed to get location", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun getColorCompat(colorResId: Int): Int {
        return ContextCompat.getColor(this, colorResId)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        handler.removeCallbacks(mapLoadTimeoutRunnable)
        handler.removeCallbacks(refreshRunnable)
        scope.cancel()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
