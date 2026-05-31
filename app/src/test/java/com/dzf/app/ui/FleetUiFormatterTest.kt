package com.dzf.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FleetUiFormatterTest {

    private val resources = FleetUiFormatter.Resources(
        readyText = "Live sync ready",
        fleetOnlineQuantity = { count ->
            if (count == 1) "%1$d device online" else "%1$d devices online"
        },
        fleetRefreshedTemplate = "%1$s • refreshed %2$ds ago"
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
}
