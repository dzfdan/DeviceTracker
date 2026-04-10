package com.dzf.app.service

import android.util.Log
import com.dzf.app.BuildConfig
import com.dzf.app.model.DeviceLocation
import com.dzf.app.util.EJsonUtil
import com.dzf.app.util.Tc3Signer
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class CloudBaseRepository {

    private val envId = BuildConfig.CLOUDBASE_ENV_ID
    private val secretId = BuildConfig.CLOUDBASE_SECRET_ID
    private val secretKey = BuildConfig.CLOUDBASE_SECRET_KEY
    private val baseUrl = "https://tcb-api.tencentcloudapi.com"
    private val collectionName = "devices"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun getAllDeviceLocations(): Result<List<DeviceLocation>> {
        return try {
            Log.d(TAG, "Fetching all devices")

            val query = mapOf<String, Any?>(
                "timestamp" to mapOf("\$gte" to getStartOfTodayMillis())
            )
            val queryJson = EJsonUtil.serialize(query, relaxed = false)

            val skip = "0"
            val limit = "1000"
            val fields = "{}"
            val sort = "{\"timestamp\":-1}"

            val url = "$baseUrl/api/v2/envs/$envId/databases/$collectionName/documents:find" +
                    "?skip=${URLEncoder.encode(skip, "UTF-8")}" +
                    "&limit=${URLEncoder.encode(limit, "UTF-8")}" +
                    "&fields=${URLEncoder.encode(fields, "UTF-8")}" +
                    "&sort=${URLEncoder.encode(sort, "UTF-8")}"

            val requestBody = gson.toJson(
                mapOf(
                    "query" to queryJson
                )
            )

            val signedHeaders = Tc3Signer.sign(
                secretId = secretId,
                secretKey = secretKey,
                httpMethod = "POST",
                url = url,
                requestBody = requestBody,
                mode = Tc3Signer.Mode.CLOUDBASE_LEGACY
            )

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("X-CloudBase-Authorization", signedHeaders.authorization)
                .addHeader("X-CloudBase-TimeStamp", signedHeaders.timestamp)
                .addHeader("X-CloudBase-SessionToken", "")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    Log.d(TAG, "Query response: $body")
                    val json = gson.fromJson(body, Map::class.java)
                    val code = json["code"] as? String
                    if (!code.isNullOrEmpty()) {
                        val message = json["message"] as? String ?: "Unknown error"
                        return Result.failure(Exception("Query failed: $code $message"))
                    }
                    val data = (json["data"] as? Map<*, *>)
                        ?: ((json["body"] as? Map<*, *>)?.get("data") as? Map<*, *>)
                    val listRaw = data?.get("list") as? List<*> ?: emptyList<Any>()
                    val list = listRaw.mapNotNull { it as? String }
                    val devices = EJsonUtil.deserializeList(list).map { doc ->
                        val docId = (doc["_id"] as? Map<*, *>)?.get("\$oid") as? String
                            ?: (doc["_id"] as? String) ?: ""
                        DeviceLocation.fromMap(doc, docId)
                    }
                    val latestDevices = devices
                        .groupBy { it.deviceId }
                        .mapNotNull { (_, locations) -> locations.maxByOrNull { it.timestamp } }

                    Log.d(TAG, "Fetched ${devices.size} records, latest devices=${latestDevices.size}")
                    Result.success(latestDevices)
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Get all devices failed: ${response.code} $errorBody")
                    Result.failure(Exception("Failed to fetch devices: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get all device locations failed", e)
            Result.failure(e)
        }
    }

    fun upsertDeviceLocation(location: DeviceLocation): Result<String> {
        return try {
            Log.d(TAG, "Uploading location record for device: ${location.deviceId}")

            cleanupOldRecords()

            val docId = URLEncoder.encode("${location.deviceId}_${location.timestamp}_${UUID.randomUUID().toString().take(8)}", "UTF-8")
            val docData = EJsonUtil.serialize(location.toMap(), relaxed = false)
            val url = "$baseUrl/api/v2/envs/$envId/databases/$collectionName/documents/$docId"
            val requestBody = gson.toJson(mapOf("data" to docData))

            val signedHeaders = Tc3Signer.sign(
                secretId = secretId,
                secretKey = secretKey,
                httpMethod = "POST",
                url = url,
                requestBody = requestBody,
                mode = Tc3Signer.Mode.CLOUDBASE_LEGACY
            )

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("X-CloudBase-Authorization", signedHeaders.authorization)
                .addHeader("X-CloudBase-TimeStamp", signedHeaders.timestamp)
                .addHeader("X-CloudBase-SessionToken", "")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Upsert failed: ${response.code} $errorBody")
                    return Result.failure(Exception("Upsert failed: ${response.code}"))
                }

                val body = response.body?.string()
                Log.d(TAG, "Upsert response: $body")
                val json = gson.fromJson(body, Map::class.java)
                val code = json["code"] as? String
                if (!code.isNullOrEmpty()) {
                    val message = json["message"] as? String ?: "Unknown error"
                    return Result.failure(Exception("Upsert failed: $code $message"))
                }

                val data = (json["data"] as? Map<*, *>)
                    ?: ((json["body"] as? Map<*, *>)?.get("data") as? Map<*, *>)
                val upsertedId = data?.get("upsertedId") as? String
                    ?: data?.get("upsert_id") as? String
                    ?: data?.get("upsertId") as? String
                    ?: (data?.get("insertedIds") as? List<*>)?.firstOrNull() as? String
                    ?: location.deviceId
                val insertedCount = (data?.get("inserted") as? Number)?.toInt() ?: 1
                Log.d(TAG, "Inserted records=$insertedCount, id=$upsertedId")
                Result.success(upsertedId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upsert device location failed", e)
            Result.failure(e)
        }
    }

    fun getDeviceTrack(deviceId: String): Result<List<DeviceLocation>> {
        return try {
            val query = mapOf<String, Any?>(
                "deviceId" to deviceId,
                "timestamp" to mapOf("\$gte" to getStartOfTodayMillis())
            )
            val queryJson = EJsonUtil.serialize(query, relaxed = false)

            val skip = "0"
            val limit = "2000"
            val fields = "{}"
            val sort = "{\"timestamp\":1}"

            val url = "$baseUrl/api/v2/envs/$envId/databases/$collectionName/documents:find" +
                    "?skip=${URLEncoder.encode(skip, "UTF-8")}" +
                    "&limit=${URLEncoder.encode(limit, "UTF-8")}" +
                    "&fields=${URLEncoder.encode(fields, "UTF-8")}" +
                    "&sort=${URLEncoder.encode(sort, "UTF-8")}"

            val requestBody = gson.toJson(mapOf("query" to queryJson))

            val signedHeaders = Tc3Signer.sign(
                secretId = secretId,
                secretKey = secretKey,
                httpMethod = "POST",
                url = url,
                requestBody = requestBody,
                mode = Tc3Signer.Mode.CLOUDBASE_LEGACY
            )

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("X-CloudBase-Authorization", signedHeaders.authorization)
                .addHeader("X-CloudBase-TimeStamp", signedHeaders.timestamp)
                .addHeader("X-CloudBase-SessionToken", "")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    return Result.failure(Exception("Get track failed: ${response.code} $errorBody"))
                }

                val body = response.body?.string()
                val json = gson.fromJson(body, Map::class.java)
                val code = json["code"] as? String
                if (!code.isNullOrEmpty()) {
                    val message = json["message"] as? String ?: "Unknown error"
                    return Result.failure(Exception("Get track failed: $code $message"))
                }

                val data = (json["data"] as? Map<*, *>)
                    ?: ((json["body"] as? Map<*, *>)?.get("data") as? Map<*, *>)
                val listRaw = data?.get("list") as? List<*> ?: emptyList<Any>()
                val list = listRaw.mapNotNull { it as? String }
                val tracks = EJsonUtil.deserializeList(list).map { doc ->
                    val docId = (doc["_id"] as? Map<*, *>)?.get("\$oid") as? String
                        ?: (doc["_id"] as? String) ?: ""
                    DeviceLocation.fromMap(doc, docId)
                }
                Result.success(tracks)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanupOldRecords() {
        try {
            val query = mapOf<String, Any?>(
                "timestamp" to mapOf("\$lt" to getStartOfTodayMillis())
            )
            val queryJson = EJsonUtil.serialize(query, relaxed = false)
            val url = "$baseUrl/api/v2/envs/$envId/databases/$collectionName/documents:deleteMany"
            val requestBody = gson.toJson(mapOf("query" to queryJson))

            val signedHeaders = Tc3Signer.sign(
                secretId = secretId,
                secretKey = secretKey,
                httpMethod = "POST",
                url = url,
                requestBody = requestBody,
                mode = Tc3Signer.Mode.CLOUDBASE_LEGACY
            )

            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .addHeader("X-CloudBase-Authorization", signedHeaders.authorization)
                .addHeader("X-CloudBase-TimeStamp", signedHeaders.timestamp)
                .addHeader("X-CloudBase-SessionToken", "")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string()
                    Log.w(TAG, "Cleanup old records failed: ${response.code} $body")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup old records error", e)
        }
    }

    private fun getStartOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    companion object {
        private const val TAG = "CloudBaseRepository"
        private val INSTANCE = CloudBaseRepository()
        fun getInstance(): CloudBaseRepository = INSTANCE
    }
}
