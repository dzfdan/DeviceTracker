package com.dzf.app.ui

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dzf.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
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
                val mainStatePanel = activity.findViewById<View>(R.id.mainStatePanel)
                val myLocationFab = activity.findViewById<View>(R.id.myLocationFab)
                val deviceCountText = activity.findViewById<TextView>(R.id.deviceCountText)

                assertNotNull(fleetOverviewCard)
                assertNotNull(networkStatusText)
                assertNotNull(mainStatePanel)
                assertNotNull(myLocationFab)
                assertNotNull(deviceCountText)
                assertEquals(0, activity.resources.getIdentifier("trackStatusCard", "id", activity.packageName))
                assertEquals(0, activity.resources.getIdentifier("trackFab", "id", activity.packageName))
                assertEquals(0, activity.resources.getIdentifier("trackInfoText", "id", activity.packageName))

                assertEquals(View.VISIBLE, fleetOverviewCard.visibility)
                assertEquals(View.VISIBLE, networkStatusText.visibility)
                assertEquals(View.VISIBLE, myLocationFab.visibility)
                assertEquals(View.GONE, mainStatePanel.visibility)
                assertEquals("0", deviceCountText.text.toString())
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
