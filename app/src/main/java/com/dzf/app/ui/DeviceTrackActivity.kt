package com.dzf.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dzf.app.R
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.PolylineOptions
import com.dzf.app.databinding.ActivityDeviceTrackBinding
import com.dzf.app.service.CloudBaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TrackMarkerSpec(
    val drawableResId: Int,
    val anchorU: Float,
    val anchorV: Float
)

class DeviceTrackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceTrackBinding
    private lateinit var aMap: AMap
    private val cloudBase = CloudBaseRepository.getInstance()
    private lateinit var deviceName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceTrackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets(applyTop = true, applyBottom = true)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME).orEmpty()
        supportActionBar?.title = if (deviceName.isBlank()) getString(R.string.track_map_title) else deviceName

        binding.trackSummaryBodyText.text = if (deviceName.isBlank()) {
            getString(R.string.track_map_title)
        } else {
            deviceName
        }
        binding.trackSummaryMetaText.text = getString(R.string.track_summary_today)
        binding.trackStateActionButton.setOnClickListener { finish() }

        binding.mapView.onCreate(savedInstanceState)
        aMap = binding.mapView.map
        aMap.uiSettings.isZoomControlsEnabled = false

        loadTrack()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadTrack() {
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID).orEmpty()
        if (deviceId.isBlank()) {
            Toast.makeText(this, "Invalid device id", Toast.LENGTH_SHORT).show()
            showTrackState(
                title = getString(R.string.track_error_title),
                body = getString(R.string.track_error_body, getString(R.string.unknown_error)),
                actionText = getString(R.string.track_return_action),
                action = ::finish
            )
            return
        }

        lifecycleScope.launch {
            hideTrackState()
            val result = withContext(Dispatchers.IO) {
                cloudBase.getDeviceTrack(deviceId)
            }

            result.fold(
                onSuccess = { track ->
                    if (track.isEmpty()) {
                        binding.trackSummaryMetaText.text = getString(R.string.track_summary_no_route)
                        showTrackState(
                            title = getString(R.string.track_empty_title),
                            body = getString(R.string.track_empty_body),
                            actionText = getString(R.string.track_return_action),
                            action = ::finish
                        )
                        return@fold
                    }

                    hideTrackState()
                    val points = track.map { LatLng(it.latitude, it.longitude) }
                    val distanceKm = calculateDistanceKm(points)
                    binding.trackSummaryMetaText.text = getString(
                        R.string.track_summary_template,
                        points.size,
                        distanceKm
                    )

                    aMap.addPolyline(
                        PolylineOptions()
                            .addAll(points)
                            .color(Color.parseColor("#59A7FF"))
                            .width(12f)
                            .geodesic(true)
                    )

                    val start = points.first()
                    val current = points.last()

                    aMap.addMarker(
                        MarkerOptions()
                            .position(start)
                            .title("Start")
                            .snippet("Track starting point")
                            .anchor(startMarkerSpec().anchorU, startMarkerSpec().anchorV)
                            .icon(createMarkerIcon(startMarkerSpec()))
                    )

                    aMap.addMarker(
                        MarkerOptions()
                            .position(current)
                            .title("Current")
                            .snippet("Latest location")
                            .anchor(currentMarkerSpec().anchorU, currentMarkerSpec().anchorV)
                            .icon(createMarkerIcon(currentMarkerSpec()))
                    )

                    val boundsBuilder = LatLngBounds.Builder()
                    points.forEach { boundsBuilder.include(it) }
                    aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
                },
                onFailure = { error ->
                    binding.trackSummaryMetaText.text = getString(R.string.track_summary_no_route)
                    showTrackState(
                        title = getString(R.string.track_error_title),
                        body = getString(
                            R.string.track_error_body,
                            error.message ?: getString(R.string.unknown_error)
                        ),
                        actionText = getString(R.string.track_retry_action),
                        action = { loadTrack() }
                    )
                }
            )
        }
    }

    private fun showTrackState(title: String, body: String, actionText: String, action: () -> Unit) {
        binding.trackStateTitleText.text = title
        binding.trackStateBodyText.text = body
        binding.trackStateActionButton.text = actionText
        binding.trackStateActionButton.contentDescription = actionText
        binding.trackStateActionButton.setOnClickListener { action() }
        binding.trackStateActionButton.visibility = View.VISIBLE
        binding.trackStatePanel.visibility = View.VISIBLE
    }

    private fun hideTrackState() {
        binding.trackStatePanel.visibility = View.GONE
        binding.trackStateActionButton.visibility = View.GONE
    }

    private fun calculateDistanceKm(points: List<LatLng>): Double {
        if (points.size < 2) {
            return 0.0
        }

        var totalMeters = 0.0
        for (index in 1 until points.size) {
            totalMeters += distanceBetween(points[index - 1], points[index])
        }
        return totalMeters / 1000.0
    }

    private fun distanceBetween(start: LatLng, end: LatLng): Double {
        val earthRadiusMeters = 6_371_000.0
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val deltaLat = Math.toRadians(end.latitude - start.latitude)
        val deltaLng = Math.toRadians(end.longitude - start.longitude)

        val sinLat = Math.sin(deltaLat / 2)
        val sinLng = Math.sin(deltaLng / 2)
        val a = sinLat * sinLat + Math.cos(lat1) * Math.cos(lat2) * sinLng * sinLng
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return earthRadiusMeters * c
    }

    internal fun startMarkerSpec() = TrackMarkerSpec(
        drawableResId = R.drawable.ic_fleet_track,
        anchorU = 5f / 24f,
        anchorV = 17f / 24f
    )

    internal fun currentMarkerSpec() = TrackMarkerSpec(
        drawableResId = R.drawable.ic_fleet_locate,
        anchorU = 0.5f,
        anchorV = 0.5f
    )

    private fun createMarkerIcon(spec: TrackMarkerSpec) = BitmapDescriptorFactory.fromBitmap(
        requireNotNull(AppCompatResources.getDrawable(this, spec.drawableResId)) {
            "Missing marker drawable: ${spec.drawableResId}"
        }.let { drawable ->
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    )

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
    }

    companion object {
        const val EXTRA_DEVICE_ID = "extra_device_id"
        const val EXTRA_DEVICE_NAME = "extra_device_name"
    }
}
