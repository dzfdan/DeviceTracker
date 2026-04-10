package com.dzf.app.ui

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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

class DeviceTrackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceTrackBinding
    private lateinit var aMap: AMap
    private val cloudBase = CloudBaseRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceTrackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        binding.closeButton.setOnClickListener { finish() }

        val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME).orEmpty()
        supportActionBar?.title = if (deviceName.isBlank()) getString(com.dzf.app.R.string.track_map_title) else "$deviceName Track"

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
            return
        }

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                cloudBase.getDeviceTrack(deviceId)
            }

            result.fold(
                onSuccess = { track ->
                    if (track.isEmpty()) {
                        Toast.makeText(this@DeviceTrackActivity, "No track data today", Toast.LENGTH_SHORT).show()
                        return@fold
                    }

                    val points = track.map { LatLng(it.latitude, it.longitude) }
                    aMap.addPolyline(
                        PolylineOptions()
                            .addAll(points)
                            .color(Color.parseColor("#1565C0"))
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
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )

                    aMap.addMarker(
                        MarkerOptions()
                            .position(current)
                            .title("Current")
                            .snippet("Latest location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )

                    val boundsBuilder = LatLngBounds.Builder()
                    points.forEach { boundsBuilder.include(it) }
                    aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
                },
                onFailure = { error ->
                    Toast.makeText(this@DeviceTrackActivity, "Load track failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
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
    }

    companion object {
        const val EXTRA_DEVICE_ID = "extra_device_id"
        const val EXTRA_DEVICE_NAME = "extra_device_name"
    }
}
