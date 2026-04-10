package com.dzf.app.model

import com.google.gson.annotations.SerializedName

data class DeviceLocation(
    @SerializedName("_id")
    val id: String = "",
    @SerializedName("deviceId")
    val deviceId: String = "",
    @SerializedName("deviceName")
    val deviceName: String = "",
    @SerializedName("latitude")
    val latitude: Double = 0.0,
    @SerializedName("longitude")
    val longitude: Double = 0.0,
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("isOnline")
    val isOnline: Boolean = true
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "deviceId" to deviceId,
            "deviceName" to deviceName,
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to timestamp,
            "isOnline" to isOnline
        )
    }

    companion object {
        private fun parseString(value: Any?): String? {
            return when (value) {
                is String -> value
                is Map<*, *> -> {
                    (value["\$oid"] as? String)
                        ?: (value["\$string"] as? String)
                }
                else -> null
            }
        }

        private fun parseDouble(value: Any?): Double? {
            return when (value) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                is Map<*, *> -> {
                    (value["\$numberDouble"] as? String)?.toDoubleOrNull()
                        ?: (value["\$numberInt"] as? String)?.toDoubleOrNull()
                        ?: (value["\$numberLong"] as? String)?.toDoubleOrNull()
                }
                else -> null
            }
        }

        private fun parseLong(value: Any?): Long? {
            return when (value) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                is Map<*, *> -> {
                    (value["\$numberLong"] as? String)?.toLongOrNull()
                        ?: (value["\$numberInt"] as? String)?.toLongOrNull()
                        ?: (value["\$numberDouble"] as? String)?.toDoubleOrNull()?.toLong()
                }
                else -> null
            }
        }

        fun fromMap(map: Map<String, Any?>, docId: String): DeviceLocation {
            return DeviceLocation(
                id = docId,
                deviceId = parseString(map["deviceId"]) ?: "",
                deviceName = parseString(map["deviceName"]) ?: "Unknown Device",
                latitude = parseDouble(map["latitude"]) ?: 0.0,
                longitude = parseDouble(map["longitude"]) ?: 0.0,
                timestamp = parseLong(map["timestamp"]) ?: System.currentTimeMillis(),
                isOnline = (map["isOnline"] as? Boolean) ?: false
            )
        }
    }
}
