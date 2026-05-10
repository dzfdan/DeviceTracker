package com.dzf.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FleetUiFormatterTest {

    private val resources = FleetUiFormatter.Resources(
        readyText = "Live sync ready",
        idleTrackText = "Standing by",
        fleetOnlineQuantity = { count ->
            if (count == 1) "%1$d device online" else "%1$d devices online"
        },
        fleetRefreshedTemplate = "%1$s • refreshed %2$ds ago",
        trackSummaryTemplate = "%1$d points • %2$.2f km"
    )

    @Test
    fun formatFleetStatusOrReady_returnsReadyCopyBeforeFirstRefresh() {
        val text = FleetUiFormatter.formatFleetStatusOrReady(
            deviceCount = 0,
            lastRefreshAgeSeconds = 0,
            hasFreshData = false,
            resources = resources
        )

        assertEquals("Live sync ready", text)
    }

    @Test
    fun formatFleetStatus_formatsSingleDevice() {
        val text = FleetUiFormatter.formatFleetStatus(
            deviceCount = 1,
            lastRefreshAgeSeconds = 12,
            resources = resources
        )

        assertEquals("1 device online • refreshed 12s ago", text)
    }

    @Test
    fun formatFleetStatus_formatsPluralDevices() {
        val text = FleetUiFormatter.formatFleetStatus(
            deviceCount = 8,
            lastRefreshAgeSeconds = 4,
            resources = resources
        )

        assertEquals("8 devices online • refreshed 4s ago", text)
    }

    @Test
    fun formatTrackSummary_formatsDistanceWithTwoDecimals() {
        val text = FleetUiFormatter.formatTrackSummary(
            pointCount = 15,
            distanceKm = 2.345,
            resources = resources
        )

        assertEquals("15 points • 2.35 km", text)
    }

    @Test
    fun formatTrackSummaryOrIdle_returnsIdleCopyWhenTrackingInactive() {
        val text = FleetUiFormatter.formatTrackSummaryOrIdle(
            pointCount = 9,
            distanceKm = 1.234,
            isTracking = false,
            resources = resources
        )

        assertEquals("Standing by", text)
    }

    @Test
    fun formatTrackSummaryOrIdle_returnsFreshSummaryWhenTrackingStarts() {
        val text = FleetUiFormatter.formatTrackSummaryOrIdle(
            pointCount = 0,
            distanceKm = 0.0,
            isTracking = true,
            resources = resources
        )

        assertEquals("0 points • 0.00 km", text)
    }

    @Test
    fun formatTrackSummaryOrIdle_returnsIdleCopyAfterTrackingStops() {
        val text = FleetUiFormatter.formatTrackSummaryOrIdle(
            pointCount = 12,
            distanceKm = 3.21,
            isTracking = false,
            resources = resources
        )

        assertEquals("Standing by", text)
    }
}
