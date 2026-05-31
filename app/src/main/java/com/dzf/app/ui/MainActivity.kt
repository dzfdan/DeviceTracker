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
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import com.dzf.app.util.CoordinateTransform
import com.dzf.app.util.DeviceInfoHelper
import com.dzf.app.util.PermissionHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

internal data class MarkerPalette(
    val colorToken: Int,
    val labelFillHex: String,
    val labelStrokeHex: String,
    val labelTextHex: String,
    val haloAlpha: Int
)

internal object MarkerUiStyle {
    val COLOR_CURRENT = R.color.marker_current
    val COLOR_ONLINE = R.color.marker_other
    val COLOR_OFFLINE = R.color.marker_offline

    const val LABEL_FILL_HIGHLIGHT = "#D924322D"
    const val LABEL_STROKE_HIGHLIGHT = "#6F98C5A7"
    const val LABEL_FILL_DEFAULT = "#D91D2A25"
    const val LABEL_STROKE_DEFAULT = "#6F4B5F58"
    const val LABEL_TEXT_DEFAULT = "#F2F5F1"
    const val LABEL_TEXT_OFFLINE = "#C2CDC5"

    fun resolve(highlight: Boolean, isOnline: Boolean): MarkerPalette {
        return when {
            highlight -> MarkerPalette(
                colorToken = COLOR_CURRENT,
                labelFillHex = LABEL_FILL_HIGHLIGHT,
                labelStrokeHex = LABEL_STROKE_HIGHLIGHT,
                labelTextHex = LABEL_TEXT_DEFAULT,
                haloAlpha = 84
            )
            isOnline -> MarkerPalette(
                colorToken = COLOR_ONLINE,
                labelFillHex = LABEL_FILL_DEFAULT,
                labelStrokeHex = LABEL_STROKE_DEFAULT,
                labelTextHex = LABEL_TEXT_DEFAULT,
                haloAlpha = 0
            )
            else -> MarkerPalette(
                colorToken = COLOR_OFFLINE,
                labelFillHex = LABEL_FILL_DEFAULT,
                labelStrokeHex = LABEL_STROKE_DEFAULT,
                labelTextHex = LABEL_TEXT_OFFLINE,
                haloAlpha = 0
            )
        }
    }

    fun formatLabel(name: String): String {
        val trimmed = name.trim()
        return if (trimmed.length > 11) "${trimmed.take(11)}..." else trimmed
    }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var aMap: AMap
    private val cloudBase = CloudBaseRepository.getInstance()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = MainScope()
    private var locationManager: AMapLocationManager? = null

    private val markers = mutableMapOf<String, Marker>()
    private val markerVisualStates = mutableMapOf<String, MarkerVisualState>()
    private val markerIconCache = mutableMapOf<MarkerVisualState, com.amap.api.maps.model.BitmapDescriptor>()
    private lateinit var deviceId: String
    private var isMapLoaded = false
    private var hasStartedTracking = false
    private lateinit var formatterResources: FleetUiFormatter.Resources

    private val trackPoints = mutableListOf<LatLng>()
    private var trackPolyline: com.amap.api.maps.model.Polyline? = null
    private var isTracking = false

    private val refreshInterval: Long = 15000
    private val mapLoadTimeoutMs: Long = 8_000
    private var lastFleetRefreshMs: Long = System.currentTimeMillis()
    private var hasFreshFleetData: Boolean = false

    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadDeviceLocations()
            handler.postDelayed(this, refreshInterval)
        }
    }

    private val mapLoadTimeoutRunnable = Runnable {
        if (!isMapLoaded) {
            Log.w(TAG, "Map load timeout. Please verify AMAP_KEY and network.")
            showMainState(
                title = getString(R.string.map_load_timeout_title),
                body = getString(R.string.map_load_timeout_hint),
            )
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
                Toast.makeText(this, getString(R.string.toast_location_permission_required), Toast.LENGTH_LONG).show()
            }
        }

    private val requestBackgroundPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Toast.makeText(
                    this,
                    getString(R.string.toast_background_location_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
            maybeStartTracking()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets(applyTop = true, applyBottom = true)

        deviceId = DeviceInfoHelper.getDeviceId(this)
        formatterResources = FleetUiFormatter.Resources(
            readyText = getString(R.string.sync_status_ready),
            idleTrackText = getString(R.string.track_status_idle),
            fleetOnlineQuantity = { count -> resources.getQuantityString(R.plurals.fleet_online_count, count) },
            fleetRefreshedTemplate = getString(R.string.fleet_status_refreshed),
            trackSummaryTemplate = getString(R.string.track_summary_compact)
        )

        updateTrackFabPresentation(isTracking = false)

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

        binding.trackFab.setOnClickListener {
            toggleTracking()
        }

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
        hideMainState()
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
        createThemedDialogBuilder()
            .setTitle(getString(R.string.location_panel_title))
            .setMessage(getString(R.string.location_panel_body))
            .setPositiveButton(getString(R.string.grant)) { _, _ ->
                requestBackgroundPermissionLauncher.launch(PermissionHelper.backgroundLocationPermission())
            }
            .setNegativeButton(getString(R.string.dismiss)) { _, _ ->
                maybeStartTracking()
            }
            .show()
    }

    private fun updateFleetHeader(deviceCount: Int) {
        val ageSeconds = ((System.currentTimeMillis() - lastFleetRefreshMs) / 1000L).coerceAtLeast(0L)
        binding.deviceCountText.text = deviceCount.toString()
        binding.fleetStatusText.text = FleetUiFormatter.formatFleetStatusOrReady(
            deviceCount = deviceCount,
            lastRefreshAgeSeconds = ageSeconds,
            hasFreshData = hasFreshFleetData,
            resources = formatterResources
        )
    }

    private fun showMainState(title: String, body: String) {
        binding.mainStateTitleText.text = title
        binding.mainStateBodyText.text = body
        binding.mainStatePanel.visibility = View.VISIBLE
    }

    private fun hideMainState() {
        binding.mainStatePanel.visibility = View.GONE
    }

    internal fun createThemedDialogBuilder(): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_DeviceTracker_Dialog)
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
            },
            updateIntervalMs = 1_000L
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
        val (gcjLat, gcjLng) = CoordinateTransform.wgs84ToGcj02(location.latitude, location.longitude)
        val rawLatLng = LatLng(gcjLat, gcjLng)
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
            myLocationMarker?.`object` = deviceId
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
        updateTrackInfo()
        updateTrackFabPresentation(isTracking = true)
        binding.trackCard.visibility = View.VISIBLE
        Toast.makeText(this, getString(R.string.tracking_started), Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Tracking started")
    }

    private fun stopTracking() {
        isTracking = false
        updateTrackInfo()
        updateTrackFabPresentation(isTracking = false)
        Toast.makeText(this, getString(R.string.tracking_stopped), Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Tracking stopped. Total points: ${trackPoints.size}")
    }

    private fun updateTrackFabPresentation(isTracking: Boolean) {
        binding.trackFab.setImageResource(
            if (isTracking) R.drawable.ic_fleet_close else R.drawable.ic_fleet_track
        )
        binding.trackFab.contentDescription = getString(
            if (isTracking) R.string.stop_tracking else R.string.start_tracking
        )
        binding.trackFab.isActivated = isTracking
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
        binding.trackInfoText.text = FleetUiFormatter.formatTrackSummaryOrIdle(
            pointCount = trackPoints.size,
            distanceKm = distance,
            isTracking = isTracking,
            resources = formatterResources
        )
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
        val palette = MarkerUiStyle.resolve(highlight = true, isOnline = true)
        return createLabeledMarkerIcon(
            name = name,
            color = getColorCompat(palette.colorToken),
            palette = palette,
            highlight = true
        )
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
                    hasFreshFleetData = true
                    lastFleetRefreshMs = System.currentTimeMillis()
                    updateMarkers(devices)
                    runOnUiThread {
                        updateFleetHeader(devices.size)
                        hideMainState()
                    }
                },
                onFailure = { error ->
                    runOnUiThread {
                        showMainState(
                            title = getString(R.string.map_refresh_failed_title),
                            body = getString(R.string.map_error_body),
                        )
                        Toast.makeText(this@MainActivity, getString(R.string.map_error_body), Toast.LENGTH_SHORT).show()
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
            markerVisualStates.remove(id)
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
                val state = buildMarkerVisualState(device)
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .title(device.deviceName)
                    .snippet(buildMarkerSnippet(device))
                    .icon(getOrCreateMarkerBitmap(state))
                val marker = aMap.addMarker(markerOptions)
                if (marker != null) {
                    marker.`object` = device.deviceId
                    markers[device.deviceId] = marker
                    markerVisualStates[device.deviceId] = state
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
        val state = buildMarkerVisualState(device)
        if (markerVisualStates[device.deviceId] == state) return

        markerVisualStates[device.deviceId] = state
        marker.setIcon(getOrCreateMarkerBitmap(state))
    }

    private fun buildMarkerVisualState(device: DeviceLocation): MarkerVisualState {
        val highlight = device.deviceId == deviceId
        val palette = MarkerUiStyle.resolve(highlight = highlight, isOnline = device.isOnline)
        return MarkerVisualState(
            label = device.deviceName.ifBlank { getString(R.string.unknown_device) },
            color = getColorCompat(palette.colorToken),
            highlight = highlight,
            palette = palette
        )
    }

    private fun getOrCreateMarkerBitmap(state: MarkerVisualState): com.amap.api.maps.model.BitmapDescriptor {
        return markerIconCache.getOrPut(state) {
            createLabeledMarkerIcon(
                name = state.label,
                color = state.color,
                palette = state.palette,
                highlight = state.highlight
            )
        }
    }

    private fun createLabeledMarkerIcon(
        name: String,
        color: Int,
        palette: MarkerPalette,
        highlight: Boolean
    ): com.amap.api.maps.model.BitmapDescriptor {
        val metrics = resources.displayMetrics
        val width = dp(metrics, 104f).toInt()
        val height = dp(metrics, 108f).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = width / 2f
        val circleCy = dp(metrics, 32f)
        val circleRadius = dp(metrics, 13f)

        if (highlight && palette.haloAlpha > 0) {
            val haloPaint = Paint().apply {
                this.color = color
                alpha = palette.haloAlpha
                isAntiAlias = true
            }
            canvas.drawCircle(cx, circleCy, circleRadius + dp(metrics, 6f), haloPaint)
        }

        val pinPaint = Paint().apply {
            this.color = color
            isAntiAlias = true
        }
        val pinSurfacePaint = Paint().apply {
            this.color = Color.parseColor("#CC0F1814")
            isAntiAlias = true
        }
        val centerPaint = Paint().apply {
            this.color = Color.parseColor("#F2F5F1")
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            this.color = Color.parseColor("#4B5F58")
            style = Paint.Style.STROKE
            strokeWidth = dp(metrics, 1.25f)
            isAntiAlias = true
        }
        canvas.drawCircle(cx, circleCy, circleRadius + dp(metrics, 3f), pinSurfacePaint)
        canvas.drawCircle(cx, circleCy, circleRadius, pinPaint)
        canvas.drawCircle(cx, circleCy, dp(metrics, 4.5f), centerPaint)
        canvas.drawCircle(cx, circleCy, circleRadius + dp(metrics, 3f), borderPaint)

        val tail = Path().apply {
            moveTo(cx, circleCy + circleRadius - dp(metrics, 1f))
            lineTo(cx - dp(metrics, 6f), circleCy + circleRadius + dp(metrics, 14f))
            lineTo(cx + dp(metrics, 6f), circleCy + circleRadius + dp(metrics, 14f))
            close()
        }
        canvas.drawPath(tail, pinPaint)

        val horizontalPadding = dp(metrics, 10f)
        val labelRect = RectF(
            horizontalPadding,
            dp(metrics, 62f),
            width - horizontalPadding,
            dp(metrics, 84f)
        )
        val labelBg = Paint().apply {
            this.color = Color.parseColor(palette.labelFillHex)
            isAntiAlias = true
        }
        val labelBorder = Paint().apply {
            this.color = Color.parseColor(palette.labelStrokeHex)
            style = Paint.Style.STROKE
            strokeWidth = dp(metrics, 1f)
            isAntiAlias = true
        }
        val labelText = Paint().apply {
            this.color = Color.parseColor(palette.labelTextHex)
            textAlign = Paint.Align.CENTER
            textSize = sp(metrics, 10.5f)
            isAntiAlias = true
        }
        val labelRadius = dp(metrics, 10f)
        canvas.drawRoundRect(labelRect, labelRadius, labelRadius, labelBg)
        canvas.drawRoundRect(labelRect, labelRadius, labelRadius, labelBorder)

        val displayName = MarkerUiStyle.formatLabel(name)
        val textY = labelRect.centerY() - ((labelText.descent() + labelText.ascent()) / 2f)
        canvas.drawText(displayName, cx, textY, labelText)

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
        } else {
            statusText.text = snippet
            timeText.text = ""
        }

        if (marker.`object` == deviceId) {
            nameText.text = "${marker.title} (${getString(R.string.this_device)})"
        }

        createThemedDialogBuilder()
            .setView(view)
            .setPositiveButton(R.string.close, null)
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
                            Toast.makeText(this@MainActivity, getString(R.string.toast_waiting_for_location), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onFailure = {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, getString(R.string.toast_failed_to_get_location), Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun getColorCompat(colorResId: Int): Int {
        return ContextCompat.getColor(this, colorResId)
    }

    private fun dp(metrics: DisplayMetrics, value: Float): Float = value * metrics.density

    private fun sp(metrics: DisplayMetrics, value: Float): Float = value * metrics.scaledDensity

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

    private data class MarkerVisualState(
        val label: String,
        val color: Int,
        val highlight: Boolean,
        val palette: MarkerPalette
    )
}
