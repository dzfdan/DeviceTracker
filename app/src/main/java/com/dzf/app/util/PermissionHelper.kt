package com.dzf.app.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionHelper {
    private val REQUIRED_PERMISSIONS = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val BACKGROUND_PERMISSION = arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    fun requiredPermissions(): Array<String> = REQUIRED_PERMISSIONS.copyOf()

    fun backgroundLocationPermission(): String = BACKGROUND_PERMISSION.first()

    fun hasLocationPermission(context: Activity): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasBackgroundLocationPermission(context: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasAllPermissions(context: Activity): Boolean {
        return hasLocationPermission(context) && hasBackgroundLocationPermission(context)
    }
}
