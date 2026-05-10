package com.dzf.app.ui

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.appbar.MaterialToolbar
import com.dzf.app.R
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceListActivityChromeTest {

    @Test
    fun deviceListScreen_rendersSingleToolbarCloseAffordanceAndRecycler() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, DeviceListActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        ActivityScenario.launch<DeviceListActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val toolbar = activity.findViewById<MaterialToolbar>(R.id.toolbar)

                assertNotNull(activity.findViewById<View>(R.id.deviceRecyclerView))
                assertNotNull(activity.findViewById<View>(R.id.listStatePanel))
                assertNotNull(toolbar)
                assertNotNull(toolbar.navigationIcon)
                assertNull(activity.findViewById<View>(R.id.closeButton))
            }
        }
    }
}
