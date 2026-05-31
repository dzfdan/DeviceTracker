package com.dzf.app.ui

object FleetUiFormatter {
    data class Resources(
        val readyText: String,
        val fleetOnlineQuantity: (Int) -> String,
        val fleetRefreshedTemplate: String
    )

    fun formatFleetStatus(
        deviceCount: Int,
        lastRefreshAgeSeconds: Long,
        resources: Resources
    ): String {
        val onlineText = resources.fleetOnlineQuantity(deviceCount).format(deviceCount)
        return resources.fleetRefreshedTemplate.format(onlineText, lastRefreshAgeSeconds)
    }

    fun formatFleetStatusOrReady(
        deviceCount: Int,
        lastRefreshAgeSeconds: Long,
        hasFreshData: Boolean,
        resources: Resources
    ): String {
        return if (hasFreshData) {
            formatFleetStatus(deviceCount, lastRefreshAgeSeconds, resources)
        } else {
            resources.readyText
        }
    }
}
