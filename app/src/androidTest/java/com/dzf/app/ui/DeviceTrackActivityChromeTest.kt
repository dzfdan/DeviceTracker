package com.dzf.app.ui

import android.content.Intent
import android.widget.TextView
import android.view.View
import com.google.android.material.button.MaterialButton
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dzf.app.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTrackActivityChromeTest {

    @Test
    fun trackScreen_withInvalidDeviceId_rendersInlineErrorChrome() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, DeviceTrackActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(DeviceTrackActivity.EXTRA_DEVICE_ID, "")
            .putExtra(DeviceTrackActivity.EXTRA_DEVICE_NAME, "Field Unit")

        ActivityScenario.launch<DeviceTrackActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val summaryCard = activity.findViewById<View>(R.id.trackSummaryCard)
                val summaryBody = activity.findViewById<TextView>(R.id.trackSummaryBodyText)
                val statePanel = activity.findViewById<View>(R.id.trackStatePanel)
                val stateTitle = activity.findViewById<TextView>(R.id.trackStateTitleText)
                val stateBody = activity.findViewById<TextView>(R.id.trackStateBodyText)
                val actionButton = activity.findViewById<MaterialButton>(R.id.trackStateActionButton)

                assertEquals(View.VISIBLE, summaryCard.visibility)
                assertEquals("Field Unit", summaryBody.text.toString())
                assertEquals(View.VISIBLE, statePanel.visibility)
                assertEquals(activity.getString(R.string.track_error_title), stateTitle.text.toString())
                assertEquals(
                    activity.getString(R.string.track_error_body, activity.getString(R.string.unknown_error)),
                    stateBody.text.toString()
                )
                assertEquals(View.VISIBLE, actionButton.visibility)
                assertEquals(activity.getString(R.string.track_return_action), actionButton.text.toString())
                assertEquals(activity.getString(R.string.track_return_action), actionButton.contentDescription?.toString())
            }
        }
    }

    @Test
    fun trackScreen_markerSpecs_usePremiumResourcesAndAnchors() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, DeviceTrackActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(DeviceTrackActivity.EXTRA_DEVICE_ID, "")
            .putExtra(DeviceTrackActivity.EXTRA_DEVICE_NAME, "Field Unit")

        ActivityScenario.launch<DeviceTrackActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(
                    TrackMarkerSpec(drawableResId = R.drawable.ic_fleet_track, anchorU = 5f / 24f, anchorV = 17f / 24f),
                    activity.startMarkerSpec()
                )
                assertEquals(
                    TrackMarkerSpec(drawableResId = R.drawable.ic_fleet_locate, anchorU = 0.5f, anchorV = 0.5f),
                    activity.currentMarkerSpec()
                )
            }
        }
    }
}
