package com.dzf.app.util

import android.content.Context
import android.provider.Settings
import java.util.UUID

object DeviceInfoHelper {
    private const val PREFS_NAME = "device_info"
    private const val KEY_DEVICE_ID = "device_id"

    private var cachedDeviceId: String? = null
    private var cachedDeviceName: String? = null

    fun getDeviceId(context: Context): String {
        cachedDeviceId?.let { return it }

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val persisted = prefs.getString(KEY_DEVICE_ID, null)
        if (!persisted.isNullOrBlank()) {
            cachedDeviceId = persisted
            return persisted
        }

        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )?.lowercase()

        val newId = if (androidId.isNullOrBlank() || androidId == "9774d56d682e549c") {
            UUID.randomUUID().toString().replace("-", "")
        } else {
            androidId
        }

        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        cachedDeviceId = newId
        return newId
    }

    fun getDeviceName(context: Context): String {
        if (cachedDeviceName == null) {
            val manufacturer = android.os.Build.MANUFACTURER
            val model = android.os.Build.MODEL
            cachedDeviceName = if (model.lowercase().startsWith(manufacturer.lowercase())) {
                model.replaceFirstChar { it.uppercase() }
            } else {
                "$manufacturer $model"
            }
        }
        return cachedDeviceName!!
    }
}
