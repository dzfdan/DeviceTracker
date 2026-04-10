package com.dzf.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.recyclerview.widget.RecyclerView
import com.dzf.app.R
import com.dzf.app.model.DeviceLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceListAdapter(
    private val items: List<DeviceLocation>,
    private val onTrackClick: (DeviceLocation) -> Unit,
    private val onExportClick: (DeviceLocation) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(items[position], timeFormat, onTrackClick, onExportClick)
    }

    override fun getItemCount(): Int = items.size

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.deviceNameText)
        private val idText: TextView = itemView.findViewById(R.id.deviceIdText)
        private val statusText: TextView = itemView.findViewById(R.id.deviceStatusText)
        private val trackButton: MaterialButton = itemView.findViewById(R.id.trackButton)
        private val exportButton: MaterialButton = itemView.findViewById(R.id.exportButton)

        fun bind(
            item: DeviceLocation,
            timeFormat: SimpleDateFormat,
            onTrackClick: (DeviceLocation) -> Unit,
            onExportClick: (DeviceLocation) -> Unit
        ) {
            nameText.text = item.deviceName.ifBlank { "Unknown Device" }
            idText.text = "ID: ${item.deviceId}"
            val state = if (item.isOnline) "Online" else "Offline"
            val time = timeFormat.format(Date(item.timestamp))
            statusText.text = "$state  |  $time"
            trackButton.setOnClickListener {
                onTrackClick(item)
            }
            exportButton.setOnClickListener {
                onExportClick(item)
            }
        }
    }
}
