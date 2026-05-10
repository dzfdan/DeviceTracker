package com.dzf.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.DisplayMetrics
import android.content.Context
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dzf.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeTokenTest {

    @Test
    fun deviceTrackerTheme_resolvesPremiumSurfaceColors() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val themed = ContextThemeWrapper(context, R.style.Theme_DeviceTracker)
        val typedValue = TypedValue()

        assertTrue(themed.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true))
        assertEquals(ContextCompat.getColor(themed, R.color.surface_primary), typedValue.data)
    }

    @Test
    fun deviceTrackerTheme_resolvesPremiumPrimaryColor() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val themed = ContextThemeWrapper(context, R.style.Theme_DeviceTracker)
        val typedValue = TypedValue()

        assertTrue(themed.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true))
        assertEquals(ContextCompat.getColor(themed, R.color.accent_icy), typedValue.data)
    }

    @Test
    fun premiumDrawablesAndIcons_areResolvable() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        val drawableIds = listOf(
            R.drawable.bg_panel_glass_primary,
            R.drawable.bg_panel_glass_secondary,
            R.drawable.bg_button_glass,
            R.drawable.bg_chip_status_online,
            R.drawable.bg_chip_status_offline,
            R.drawable.bg_state_panel,
            R.drawable.ic_fleet_locate,
            R.drawable.ic_fleet_track,
            R.drawable.ic_fleet_close,
            R.drawable.ic_fleet_export,
            R.drawable.ic_launcher_foreground,
            R.mipmap.ic_launcher,
            R.mipmap.ic_launcher_round,
        )

        drawableIds.forEach { id ->
            val drawable = ContextCompat.getDrawable(context, id)
            assertTrue("Drawable $id should resolve", drawable != null)
        }

        assertEquals(
            0xFF06101C.toInt(),
            ContextCompat.getColor(targetContext, R.color.ic_launcher_background),
        )
    }

    @Test
    fun legacyLauncherMipmaps_useSignatureArtworkAcrossDensities() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val densities = listOf(
            DisplayMetrics.DENSITY_MEDIUM,
            DisplayMetrics.DENSITY_HIGH,
            DisplayMetrics.DENSITY_XHIGH,
            DisplayMetrics.DENSITY_XXHIGH,
            DisplayMetrics.DENSITY_XXXHIGH,
        )

        listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round).forEach { id ->
            densities.forEach { density ->
                val drawable = ResourcesCompat.getDrawableForDensity(context.resources, id, density, context.theme)
                assertTrue("Legacy launcher drawable $id should resolve for density $density", drawable != null)

                val bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable!!.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)

                assertEquals(
                    "Legacy launcher drawable $id should not keep the old circular fill at density $density",
                    0,
                    bitmap.getPixel(5, 24) ushr 24,
                )
                assertTrue(
                    "Legacy launcher drawable $id should render signature artwork at density $density",
                    (bitmap.getPixel(24, 24) ushr 24) > 0,
                )
            }
        }
    }
}
