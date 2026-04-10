package com.dzf.app.util

import com.google.gson.Gson
import com.google.gson.JsonObject

object EJsonUtil {

    private val gson = Gson()

    fun serialize(data: Map<String, Any?>, relaxed: Boolean = false): String {
        val ejsonMap = convertToEJson(data, relaxed)
        return gson.toJson(ejsonMap)
    }

    fun serialize(data: Any, relaxed: Boolean = false): String {
        val ejsonMap = if (data is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            convertToEJson(data as Map<String, Any?>, relaxed)
        } else {
            val json = gson.toJsonTree(data).asJsonObject
            val map = gson.fromJson(json, Map::class.java) as Map<String, Any?>
            convertToEJson(map, relaxed)
        }
        return gson.toJson(ejsonMap)
    }

    private fun convertToEJson(data: Map<String, Any?>, relaxed: Boolean): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        for ((key, value) in data) {
            result[key] = convertValue(value, relaxed)
        }
        return result
    }

    private fun convertValue(value: Any?, relaxed: Boolean): Any? {
        return when (value) {
            null -> null
            is String -> if (relaxed) value else value
            is Number -> {
                if (relaxed) {
                    value
                } else {
                    when {
                        value is Double || value is Float -> mapOf("\$numberDouble" to value.toString())
                        value is Long -> mapOf("\$numberLong" to value.toString())
                        value is Int -> mapOf("\$numberInt" to value.toString())
                        else -> value
                    }
                }
            }
            is Boolean -> value
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                convertToEJson(value as Map<String, Any?>, relaxed)
            }
            is List<*> -> value.map { convertValue(it, relaxed) }
            is Array<*> -> value.map { convertValue(it, relaxed) }
            else -> value.toString()
        }
    }

    fun <T> deserialize(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }

    fun deserializeList(jsonArray: List<String>): List<Map<String, Any?>> {
        return jsonArray.map { item ->
            gson.fromJson(item, Map::class.java) as Map<String, Any?>
        }
    }
}
