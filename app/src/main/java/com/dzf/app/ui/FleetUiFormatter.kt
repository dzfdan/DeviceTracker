package com.dzf.app.ui

object FleetUiFormatter {
    data class Resources(
        val readyText: String,
        val idleTrackText: String,
        val fleetOnlineQuantity: (Int) -> String,
        val fleetRefreshedTemplate: String,
        val trackSummaryTemplate: String
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

    fun formatTrackSummary(
        pointCount: Int,
        distanceKm: Double,
        resources: Resources
    ): String {
        return resources.trackSummaryTemplate.format(pointCount, distanceKm)
    }

    fun formatTrackSummaryOrIdle(
        pointCount: Int,
        distanceKm: Double,
        isTracking: Boolean,
        resources: Resources
    ): String {
        return if (isTracking) {
            formatTrackSummary(pointCount, distanceKm, resources)
        } else {
            resources.idleTrackText
        }
    }
}
