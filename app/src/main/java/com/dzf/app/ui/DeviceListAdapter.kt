package com.dzf.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.dzf.app.R
import com.dzf.app.model.DeviceLocation
import com.google.android.material.button.MaterialButton
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
        private val statusChip: TextView = itemView.findViewById(R.id.deviceStatusChip)
        private val statusText: TextView = itemView.findViewById(R.id.deviceStatusText)
        private val trackButton: MaterialButton = itemView.findViewById(R.id.trackButton)
        private val exportButton: MaterialButton = itemView.findViewById(R.id.exportButton)

        fun bind(
            item: DeviceLocation,
            timeFormat: SimpleDateFormat,
            onTrackClick: (DeviceLocation) -> Unit,
            onExportClick: (DeviceLocation) -> Unit
        ) {
            val context = itemView.context
            val state = if (item.isOnline) context.getString(R.string.online) else context.getString(R.string.offline)
            val time = timeFormat.format(Date(item.timestamp))
            nameText.text = item.deviceName.ifBlank { context.getString(R.string.unknown_device) }
            statusText.text = context.getString(R.string.last_seen, time)
            idText.text = context.getString(R.string.device_id_value, item.deviceId)
            statusChip.text = state
            statusChip.setBackgroundResource(R.drawable.bg_status_chip)
            statusChip.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (item.isOnline) R.color.accent_positive else R.color.text_tertiary
                )
            )
            trackButton.setOnClickListener {
                onTrackClick(item)
            }
            exportButton.setOnClickListener {
                onExportClick(item)
            }
        }
    }
}
