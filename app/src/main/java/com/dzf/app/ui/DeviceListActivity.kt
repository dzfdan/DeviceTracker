package com.dzf.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.dzf.app.R
import com.dzf.app.model.DeviceLocation
import com.dzf.app.databinding.ActivityDeviceListBinding
import com.dzf.app.service.CloudBaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceListBinding
    private val cloudBase = CloudBaseRepository.getInstance()
    private val exportTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets(applyTop = true, applyBottom = true)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        binding.deviceRecyclerView.layoutManager = LinearLayoutManager(this)
        loadDevices()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadDevices() {
        lifecycleScope.launch {
            hideListState()
            val result = withContext(Dispatchers.IO) {
                cloudBase.getAllDeviceLocations()
            }

            result.fold(
                onSuccess = { devices ->
                    hideListState()
                    binding.deviceRecyclerView.adapter = DeviceListAdapter(
                        items = devices,
                        onTrackClick = { device ->
                            val intent = Intent(this@DeviceListActivity, DeviceTrackActivity::class.java)
                            intent.putExtra(DeviceTrackActivity.EXTRA_DEVICE_ID, device.deviceId)
                            intent.putExtra(DeviceTrackActivity.EXTRA_DEVICE_NAME, device.deviceName)
                            startActivity(intent)
                        },
                        onExportClick = { device -> exportTimeline(device) }
                    )

                    if (devices.isEmpty()) {
                        showListState(
                            getString(R.string.device_list_empty_title),
                            getString(R.string.device_list_empty_body)
                        )
                    }
                },
                onFailure = { error ->
                    binding.deviceRecyclerView.adapter = DeviceListAdapter(
                        items = emptyList(),
                        onTrackClick = { },
                        onExportClick = { }
                    )
                    showListState(
                        getString(R.string.device_list_load_failed_title),
                        getString(R.string.device_list_load_failed_body, error.message ?: getString(R.string.unknown_error))
                    )
                }
            )
        }
    }

    private fun showListState(title: String, body: String) {
        binding.listStateTitleText.text = title
        binding.listStateBodyText.text = body
        binding.listStatePanel.visibility = View.VISIBLE
    }

    private fun hideListState() {
        binding.listStatePanel.visibility = View.GONE
    }

    private fun exportTimeline(device: DeviceLocation) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                cloudBase.getDeviceTrack(device.deviceId)
            }

            result.fold(
                onSuccess = { track ->
                    if (track.isEmpty()) {
                        Toast.makeText(this@DeviceListActivity, getString(R.string.export_empty_timeline), Toast.LENGTH_SHORT).show()
                        return@fold
                    }

                    val content = buildTimelineCsv(device.deviceName, device.deviceId, track)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            "${getString(R.string.export_file_prefix)}_${sanitizeFileName(device.deviceName)}_${todayDateString()}"
                        )
                        putExtra(Intent.EXTRA_TEXT, content)
                    }
                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share_timeline_title)))
                },
                onFailure = { error ->
                    val message = error.message ?: "unknown"
                    Toast.makeText(this@DeviceListActivity, getString(R.string.export_failed, message), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun buildTimelineCsv(deviceName: String, deviceId: String, track: List<DeviceLocation>): String {
        val sb = StringBuilder()
        sb.append("deviceName,deviceId,time,latitude,longitude,isOnline\n")
        track.forEach { point ->
            sb.append(escapeCsv(deviceName)).append(',')
                .append(escapeCsv(deviceId)).append(',')
                .append(escapeCsv(exportTimeFormat.format(Date(point.timestamp)))).append(',')
                .append(point.latitude).append(',')
                .append(point.longitude).append(',')
                .append(point.isOnline)
                .append('\n')
        }
        return sb.toString()
    }

    private fun sanitizeFileName(input: String): String {
        return input.ifBlank { "unknown_device" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .trim('_')
            .ifBlank { "unknown_device" }
    }

    private fun todayDateString(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
