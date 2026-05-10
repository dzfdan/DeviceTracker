package com.dzf.app.ui

import android.widget.TextView
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dzf.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityChromeTest {

    @Test
    fun homeScreen_rendersFleetChromeShellContract() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Full activity launch is still required by current architecture because the shell
                // is inflated by the real activity and view binding lifecycle.
                val fleetOverviewCard = activity.findViewById<View>(R.id.fleetOverviewCard)
                val networkStatusText = activity.findViewById<View>(R.id.networkStatusText)
                val trackStatusCard = activity.findViewById<View>(R.id.trackStatusCard)
                val mainStatePanel = activity.findViewById<View>(R.id.mainStatePanel)
                val trackFab = activity.findViewById<View>(R.id.trackFab)
                val myLocationFab = activity.findViewById<View>(R.id.myLocationFab)
                val trackInfoText = activity.findViewById<TextView>(R.id.trackInfoText)
                val deviceCountText = activity.findViewById<TextView>(R.id.deviceCountText)

                assertNotNull(fleetOverviewCard)
                assertNotNull(networkStatusText)
                assertNotNull(trackStatusCard)
                assertNotNull(mainStatePanel)
                assertNotNull(trackFab)
                assertNotNull(myLocationFab)
                assertNotNull(trackInfoText)
                assertNotNull(deviceCountText)

                assertEquals(View.VISIBLE, fleetOverviewCard.visibility)
                assertEquals(View.VISIBLE, networkStatusText.visibility)
                assertEquals(View.VISIBLE, trackStatusCard.visibility)
                assertEquals(View.VISIBLE, trackFab.visibility)
                assertEquals(View.VISIBLE, myLocationFab.visibility)
                assertEquals(View.GONE, mainStatePanel.visibility)
                assertEquals(activity.getString(R.string.track_status_idle), trackInfoText.text.toString())
                assertEquals("0", deviceCountText.text.toString())
                assertEquals(activity.getString(R.string.start_tracking), trackFab.contentDescription.toString())
            }
        }
    }

    @Test
    fun mainActivity_exposesMaterialDialogBuilderAndDedicatedRefreshFailureTitle() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(
                    activity.createThemedDialogBuilder()
                        is com.google.android.material.dialog.MaterialAlertDialogBuilder
                )
                assertNotEquals(
                    activity.getString(R.string.map_load_timeout_title),
                    activity.getString(R.string.map_refresh_failed_title)
                )
            }
        }
    }
}
