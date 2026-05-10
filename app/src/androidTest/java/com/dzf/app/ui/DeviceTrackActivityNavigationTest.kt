package com.dzf.app.ui

import android.content.Intent
import android.view.ViewGroup
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dzf.app.R
import com.google.android.material.appbar.MaterialToolbar
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTrackActivityNavigationTest {

    @Test
    fun trackScreen_usesToolbarNavigationAsOnlyCloseAffordance() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, DeviceTrackActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(DeviceTrackActivity.EXTRA_DEVICE_ID, "")
            .putExtra(DeviceTrackActivity.EXTRA_DEVICE_NAME, "Field Unit")

        ActivityScenario.launch<DeviceTrackActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val toolbar = activity.findViewById<MaterialToolbar>(R.id.toolbar)
                val closeLabel = activity.getString(R.string.close)

                assertNotNull(toolbar)
                assertNotNull(toolbar.navigationIcon)
                assertFalse(hasVisibleViewWithContentDescription(activity.findViewById(android.R.id.content), closeLabel))
            }
        }
    }

    private fun hasVisibleViewWithContentDescription(root: View, label: String): Boolean {
        if (root.visibility == View.VISIBLE && root.contentDescription?.toString() == label) {
            return true
        }

        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                if (hasVisibleViewWithContentDescription(root.getChildAt(index), label)) {
                    return true
                }
            }
        }

        return false
    }
}
