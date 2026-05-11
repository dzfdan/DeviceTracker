# DeviceTracker Visual Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild DeviceTracker's Android UI into a premium dark map-first experience with redesigned overlays, states, icons, launcher branding, and markers while keeping the current Activity/XML architecture.

**Architecture:** Keep the existing View-based app structure and upgrade it in place. Centralize visual tokens in resources, use a small pure Kotlin formatter helper for repeated status copy, and update each Activity/layout incrementally so every commit leaves the app buildable.

**Tech Stack:** Android Views/XML, Material 3, Kotlin, AMap SDK, custom vector drawables, Android instrumentation tests, JUnit 4

---

## File Structure

### Resource foundation

- Modify: `app/build.gradle`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/dimens.xml`
- Create: `app/src/main/res/values/styles.xml`

### Shared visual assets

- Create: `app/src/main/res/drawable/bg_panel_glass_primary.xml`
- Create: `app/src/main/res/drawable/bg_panel_glass_secondary.xml`
- Create: `app/src/main/res/drawable/bg_button_glass.xml`
- Create: `app/src/main/res/drawable/bg_chip_status_online.xml`
- Create: `app/src/main/res/drawable/bg_chip_status_offline.xml`
- Create: `app/src/main/res/drawable/bg_state_panel.xml`

### Iconography and launcher

- Create: `app/src/main/res/drawable/ic_fleet_locate.xml`
- Create: `app/src/main/res/drawable/ic_fleet_track.xml`
- Create: `app/src/main/res/drawable/ic_fleet_close.xml`
- Create: `app/src/main/res/drawable/ic_fleet_export.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/values/ic_launcher_background.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

### UI and behavior

- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/layout/activity_device_list.xml`
- Modify: `app/src/main/res/layout/activity_device_track.xml`
- Modify: `app/src/main/res/layout/item_device.xml`
- Create: `app/src/main/java/com/dzf/app/ui/FleetUiFormatter.kt`
- Modify: `app/src/main/java/com/dzf/app/ui/MainActivity.kt`
- Modify: `app/src/main/java/com/dzf/app/ui/DeviceListActivity.kt`
- Modify: `app/src/main/java/com/dzf/app/ui/DeviceListAdapter.kt`
- Modify: `app/src/main/java/com/dzf/app/ui/DeviceTrackActivity.kt`

### Tests

- Create: `app/src/test/java/com/dzf/app/ui/FleetUiFormatterTest.kt`
- Create: `app/src/androidTest/java/com/dzf/app/ui/ThemeTokenTest.kt`
- Create: `app/src/androidTest/java/com/dzf/app/ui/MainActivityChromeTest.kt`
- Create: `app/src/androidTest/java/com/dzf/app/ui/DeviceListActivityChromeTest.kt`
- Create: `app/src/androidTest/java/com/dzf/app/ui/DeviceTrackActivityChromeTest.kt`

---

### Task 1: Establish Premium Theme Tokens And Resource Test Harness

**Files:**
- Modify: `app/build.gradle`
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/dimens.xml`
- Create: `app/src/main/res/values/styles.xml`
- Create: `app/src/androidTest/java/com/dzf/app/ui/ThemeTokenTest.kt`

- [ ] **Step 1: Write the failing instrumentation test for theme tokens**

```kotlin
package com.dzf.app.ui

import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
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
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val themed = ContextThemeWrapper(context, R.style.Theme_DeviceTracker)
        val typedValue = TypedValue()

        assertTrue(themed.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true))
        assertEquals(ContextCompat.getColor(themed, R.color.surface_primary), typedValue.data)
    }

    @Test
    fun deviceTrackerTheme_resolvesPremiumPrimaryColor() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val themed = ContextThemeWrapper(context, R.style.Theme_DeviceTracker)
        val typedValue = TypedValue()

        assertTrue(themed.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true))
        assertEquals(ContextCompat.getColor(themed, R.color.accent_icy), typedValue.data)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails with missing tokens**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dzf.app.ui.ThemeTokenTest`

Expected: FAIL because `surface_primary` and `accent_icy` do not exist and `colorSurface` is not overridden.

- [ ] **Step 3: Add the new color, spacing, and style resources**

`app/build.gradle`

```groovy
androidTestImplementation 'androidx.test:core-ktx:1.5.0'
androidTestImplementation 'androidx.test:rules:1.5.0'
```

`app/src/main/res/values/colors.xml`

```xml
<resources>
    <color name="surface_primary">#08111E</color>
    <color name="surface_secondary">#102033</color>
    <color name="surface_panel">#CC12263A</color>
    <color name="surface_panel_secondary">#B3152B44</color>
    <color name="surface_state">#DD132237</color>
    <color name="stroke_subtle">#33D9ECFF</color>
    <color name="stroke_strong">#4DD9ECFF</color>
    <color name="text_primary">#F2F7FF</color>
    <color name="text_secondary">#A9BCD2</color>
    <color name="text_muted">#73859C</color>
    <color name="accent_icy">#8EEBFF</color>
    <color name="accent_blue">#59A7FF</color>
    <color name="accent_warning">#F2B766</color>
    <color name="accent_danger">#D96C6C</color>
    <color name="marker_current">#B7F6FF</color>
    <color name="marker_other">#61B6FF</color>
    <color name="marker_offline">#718399</color>
</resources>
```

`app/src/main/res/values/dimens.xml`

```xml
<resources>
    <dimen name="space_xs">6dp</dimen>
    <dimen name="space_sm">10dp</dimen>
    <dimen name="space_md">16dp</dimen>
    <dimen name="space_lg">20dp</dimen>
    <dimen name="space_xl">28dp</dimen>
    <dimen name="radius_chip">14dp</dimen>
    <dimen name="radius_panel">24dp</dimen>
    <dimen name="radius_button">22dp</dimen>
    <dimen name="fab_shell_size">60dp</dimen>
</resources>
```

`app/src/main/res/values/styles.xml`

```xml
<resources>
    <style name="TextAppearance.DeviceTracker.Overline" parent="TextAppearance.Material3.LabelSmall">
        <item name="android:textAllCaps">true</item>
        <item name="android:letterSpacing">0.18</item>
        <item name="android:textColor">@color/text_secondary</item>
    </style>

    <style name="TextAppearance.DeviceTracker.Metric" parent="TextAppearance.Material3.DisplaySmall">
        <item name="android:textStyle">bold</item>
        <item name="android:textColor">@color/text_primary</item>
    </style>

    <style name="Widget.DeviceTracker.Toolbar" parent="Widget.Material3.Toolbar">
        <item name="android:background">@android:color/transparent</item>
        <item name="titleTextColor">@color/text_primary</item>
        <item name="subtitleTextColor">@color/text_secondary</item>
        <item name="navigationIconTint">@color/text_primary</item>
    </style>
    <style name="ThemeOverlay.DeviceTracker.Dialog" parent="ThemeOverlay.Material3.MaterialAlertDialog">
        <item name="colorPrimary">@color/accent_icy</item>
        <item name="android:textColorPrimary">@color/text_primary</item>
        <item name="android:windowBackground">@drawable/bg_panel_glass_primary</item>
    </style>
 </resources>
```

`app/src/main/res/values/themes.xml`

```xml
<style name="Theme.DeviceTracker" parent="Theme.Material3.DayNight.NoActionBar">
    <item name="colorPrimary">@color/accent_icy</item>
    <item name="colorSecondary">@color/accent_blue</item>
    <item name="colorSurface">@color/surface_primary</item>
    <item name="colorBackground">@color/surface_primary</item>
    <item name="android:windowBackground">@color/surface_primary</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@color/surface_primary</item>
    <item name="materialAlertDialogTheme">@style/ThemeOverlay.DeviceTracker.Dialog</item>
</style>
```

`app/src/main/res/values/strings.xml`

```xml
<string name="fleet_online">Fleet Online</string>
<string name="live_location_network">Live location network</string>
<string name="sync_status_ready">Live sync ready</string>
<string name="location_panel_title">Location access needed</string>
<string name="location_panel_body">Allow background location so this device stays visible on the live map when the app is not on screen.</string>
<string name="dismiss">Dismiss</string>
```

- [ ] **Step 4: Run the test again and verify the new theme tokens resolve**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dzf.app.ui.ThemeTokenTest`

Expected: PASS for `ThemeTokenTest`.

- [ ] **Step 5: Commit the resource foundation**

```bash
git add app/build.gradle app/src/main/res/values/colors.xml app/src/main/res/values/dimens.xml app/src/main/res/values/styles.xml app/src/main/res/values/themes.xml app/src/main/res/values/strings.xml app/src/androidTest/java/com/dzf/app/ui/ThemeTokenTest.kt
git commit -m "feat: add premium visual theme tokens"
```

### Task 2: Add Glass Background Assets And Signature Icon System

**Files:**
- Create: `app/src/main/res/drawable/bg_panel_glass_primary.xml`
- Create: `app/src/main/res/drawable/bg_panel_glass_secondary.xml`
- Create: `app/src/main/res/drawable/bg_button_glass.xml`
- Create: `app/src/main/res/drawable/bg_chip_status_online.xml`
- Create: `app/src/main/res/drawable/bg_chip_status_offline.xml`
- Create: `app/src/main/res/drawable/bg_state_panel.xml`
- Create: `app/src/main/res/drawable/ic_fleet_locate.xml`
- Create: `app/src/main/res/drawable/ic_fleet_track.xml`
- Create: `app/src/main/res/drawable/ic_fleet_close.xml`
- Create: `app/src/main/res/drawable/ic_fleet_export.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/values/ic_launcher_background.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Modify: `app/src/androidTest/java/com/dzf/app/ui/ThemeTokenTest.kt`

- [ ] **Step 1: Extend the failing test to assert custom drawables exist**

Append this test to `app/src/androidTest/java/com/dzf/app/ui/ThemeTokenTest.kt`:

```kotlin
@Test
fun premiumDrawablesAndIcons_areResolvable() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    val drawableIds = listOf(
        R.drawable.bg_panel_glass_primary,
        R.drawable.bg_button_glass,
        R.drawable.ic_fleet_locate,
        R.drawable.ic_fleet_track,
        R.drawable.ic_fleet_close,
        R.drawable.ic_fleet_export,
        R.mipmap.ic_launcher
    )

    drawableIds.forEach { id ->
        val drawable = ContextCompat.getDrawable(context, id)
        assertTrue("Drawable $id should resolve", drawable != null)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails because the assets are missing**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dzf.app.ui.ThemeTokenTest`

Expected: FAIL because the drawable and mipmap resources do not yet exist.

- [ ] **Step 3: Create the glass backgrounds, feature icons, and adaptive launcher assets**

`app/src/main/res/drawable/bg_panel_glass_primary.xml`

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="24dp" />
    <solid android:color="@color/surface_panel" />
    <stroke android:width="1dp" android:color="@color/stroke_strong" />
    <padding android:left="16dp" android:top="16dp" android:right="16dp" android:bottom="16dp" />
</shape>
```

`app/src/main/res/drawable/bg_panel_glass_secondary.xml`

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="20dp" />
    <solid android:color="@color/surface_panel_secondary" />
    <stroke android:width="1dp" android:color="@color/stroke_subtle" />
</shape>
```

`app/src/main/res/drawable/bg_button_glass.xml`

```xml
<ripple xmlns:android="http://schemas.android.com/apk/res/android" android:color="#33FFFFFF">
    <item>
        <shape android:shape="oval">
            <solid android:color="@color/surface_panel_secondary" />
            <stroke android:width="1dp" android:color="@color/stroke_strong" />
            <size android:width="60dp" android:height="60dp" />
        </shape>
    </item>
</ripple>
```

`app/src/main/res/drawable/bg_chip_status_online.xml`

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="14dp" />
    <solid android:color="#2232B8D6" />
    <stroke android:width="1dp" android:color="#6659E6FF" />
</shape>
```

`app/src/main/res/drawable/bg_chip_status_offline.xml`

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="14dp" />
    <solid android:color="#1E73859C" />
    <stroke android:width="1dp" android:color="#4473859C" />
</shape>
```

`app/src/main/res/drawable/bg_state_panel.xml`

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <corners android:radius="24dp" />
    <gradient
        android:angle="270"
        android:startColor="#E61A2F46"
        android:endColor="#F008111E" />
    <stroke android:width="1dp" android:color="#33D9ECFF" />
</shape>
```

`app/src/main/res/drawable/ic_fleet_locate.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path android:fillColor="#8EEBFF" android:pathData="M12,2L13.6,8.4L20,10L13.6,11.6L12,18L10.4,11.6L4,10L10.4,8.4Z"/>
    <path android:fillColor="#F2F7FF" android:pathData="M12,9.5A2.5,2.5 0 1,0 12,14.5A2.5,2.5 0 1,0 12,9.5Z"/>
</vector>
```

`app/src/main/res/drawable/ic_fleet_track.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path android:fillColor="#00000000" android:strokeWidth="1.8" android:strokeColor="#8EEBFF" android:pathData="M5,17C8.5,17 8.5,7 12,7s3.5,10 7,10"/>
    <path android:fillColor="#F2F7FF" android:pathData="M5,15.5A1.5,1.5 0 1,0 5,18.5A1.5,1.5 0 1,0 5,15.5Z"/>
    <path android:fillColor="#8EEBFF" android:pathData="M12,5.5A1.5,1.5 0 1,0 12,8.5A1.5,1.5 0 1,0 12,5.5Z"/>
    <path android:fillColor="#59A7FF" android:pathData="M19,15.5A1.5,1.5 0 1,0 19,18.5A1.5,1.5 0 1,0 19,15.5Z"/>
</vector>
```

`app/src/main/res/drawable/ic_fleet_close.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp"
    android:height="20dp"
    android:viewportWidth="20"
    android:viewportHeight="20">
    <path android:fillColor="#00000000" android:strokeWidth="1.8" android:strokeColor="#F2F7FF" android:pathData="M5,5L15,15"/>
    <path android:fillColor="#00000000" android:strokeWidth="1.8" android:strokeColor="#F2F7FF" android:pathData="M15,5L5,15"/>
</vector>
```

`app/src/main/res/drawable/ic_fleet_export.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="20dp"
    android:height="20dp"
    android:viewportWidth="20"
    android:viewportHeight="20">
    <path android:fillColor="#00000000" android:strokeWidth="1.8" android:strokeColor="#8EEBFF" android:pathData="M10,3V11"/>
    <path android:fillColor="#00000000" android:strokeWidth="1.8" android:strokeColor="#8EEBFF" android:pathData="M6.5,7.5L10,11L13.5,7.5"/>
    <path android:fillColor="#00000000" android:strokeWidth="1.8" android:strokeColor="#F2F7FF" android:pathData="M4,14H16"/>
</vector>
```

`app/src/main/res/drawable/ic_launcher_foreground.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#0A1320" android:pathData="M18,18h72a18,18 0 0 1 18,18v36a18,18 0 0 1 -18,18H18A18,18 0 0 1 0,72V36A18,18 0 0 1 18,18z"/>
    <path android:fillColor="#8EEBFF" android:pathData="M54,24l7,21 21,7 -21,7 -7,21 -7,-21 -21,-7 21,-7z"/>
    <path android:fillColor="#59A7FF" android:pathData="M24,74c9,-10 18,-10 27,-20 6,-7 9,-16 22,-20l6,6c-12,5 -15,13 -21,20 -10,12 -20,12 -30,22z"/>
    <path android:fillColor="#F2F7FF" android:pathData="M54,46a6,6 0 1,0 0,12a6,6 0 1,0 0,-12z"/>
</vector>
```

`app/src/main/res/values/ic_launcher_background.xml`

```xml
<resources>
    <color name="ic_launcher_background">#06101C</color>
</resources>
```

`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`

```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

`app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

```xml
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

- [ ] **Step 4: Run the drawable resolution test again**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dzf.app.ui.ThemeTokenTest`

Expected: PASS for theme and drawable resolution.

- [ ] **Step 5: Commit the asset system**

```bash
git add app/src/main/res/drawable app/src/main/res/mipmap-anydpi-v26 app/src/main/res/values/ic_launcher_background.xml app/src/androidTest/java/com/dzf/app/ui/ThemeTokenTest.kt
git commit -m "feat: add signature icon and glass asset system"
```

### Task 3: Redesign The Home Screen Layout Shell

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/androidTest/java/com/dzf/app/ui/MainActivityChromeTest.kt`

- [ ] **Step 1: Write the failing instrumentation test for the new home chrome**

```kotlin
package com.dzf.app.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dzf.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityChromeTest {

    @Test
    fun homeScreen_rendersFleetChromeViews() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById(R.id.fleetOverviewCard))
                assertNotNull(activity.findViewById(R.id.networkStatusText))
                assertNotNull(activity.findViewById(R.id.trackStatusCard))
                assertNotNull(activity.findViewById(R.id.mainStatePanel))
                assertEquals(android.view.View.GONE, activity.findViewById<android.view.View>(R.id.mainStatePanel).visibility)
            }
        }
    }
}
```

- [ ] **Step 2: Run the test and verify it fails because the ids do not exist**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dzf.app.ui.MainActivityChromeTest`

Expected: FAIL because `fleetOverviewCard`, `networkStatusText`, `trackStatusCard`, and `mainStatePanel` are missing from `activity_main.xml`.

- [ ] **Step 3: Replace the home layout with a map-first glass console shell**

`app/src/main/res/layout/activity_main.xml`

```xml
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/surface_primary">

    <com.amap.api.maps.MapView
        android:id="@+id/mapView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <View
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#2406111E" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="top"
        android:orientation="vertical"
        android:paddingStart="20dp"
        android:paddingTop="20dp"
        android:paddingEnd="20dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="DeviceTracker"
                    android:textColor="@color/text_primary"
                    android:textSize="18sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/networkStatusText"
                    style="@style/TextAppearance.DeviceTracker.Overline"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="@string/live_location_network" />
            </LinearLayout>
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="18dp"
            android:gravity="top"
            android:orientation="horizontal">

            <LinearLayout
                android:id="@+id/fleetOverviewCard"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:background="@drawable/bg_panel_glass_primary"
                android:orientation="vertical"
                android:padding="18dp">

                <TextView
                    android:id="@+id/fleetLabelText"
                    style="@style/TextAppearance.DeviceTracker.Overline"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/fleet_online" />

                <TextView
                    android:id="@+id/deviceCountText"
                    style="@style/TextAppearance.DeviceTracker.Metric"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:text="0" />

                <TextView
                    android:id="@+id/fleetStatusText"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:text="@string/sync_status_ready"
                    android:textColor="@color/text_secondary"
                    android:textSize="13sp" />
            </LinearLayout>

            <LinearLayout
                android:id="@+id/trackStatusCard"
                android:layout_width="132dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="12dp"
                android:background="@drawable/bg_panel_glass_secondary"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    style="@style/TextAppearance.DeviceTracker.Overline"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/track_recording" />

                <TextView
                    android:id="@+id/trackInfoText"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:textColor="@color/text_primary"
                    android:textSize="14sp"
                    android:textStyle="bold" />
            </LinearLayout>
        </LinearLayout>

        <LinearLayout
            android:id="@+id/mainStatePanel"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:background="@drawable/bg_state_panel"
            android:orientation="vertical"
            android:padding="18dp"
            android:visibility="gone">

            <TextView
                android:id="@+id/mainStateTitleText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/text_primary"
                android:textSize="16sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/mainStateBodyText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:textColor="@color/text_secondary"
                android:textSize="14sp" />
        </LinearLayout>
    </LinearLayout>

    <ImageButton
        android:id="@+id/trackFab"
        android:layout_width="60dp"
        android:layout_height="60dp"
        android:layout_gravity="bottom|start"
        android:layout_marginStart="20dp"
        android:layout_marginBottom="24dp"
        android:background="@drawable/bg_button_glass"
        android:contentDescription="@string/track_recording"
        android:padding="16dp"
        android:src="@drawable/ic_fleet_track"
        app:tint="@color/text_primary" />

    <ImageButton
        android:id="@+id/myLocationFab"
        android:layout_width="60dp"
        android:layout_height="60dp"
        android:layout_gravity="bottom|end"
        android:layout_marginEnd="20dp"
        android:layout_marginBottom="24dp"
        android:background="@drawable/bg_button_glass"
        android:contentDescription="@string/my_location"
        android:padding="16dp"
        android:src="@drawable/ic_fleet_locate"
        app:tint="@color/text_primary" />
</FrameLayout>
```

- [ ] **Step 4: Run the home chrome test again**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dzf.app.ui.MainActivityChromeTest`

Expected: PASS for the view-presence assertions.

- [ ] **Step 5: Commit the new home shell**

```bash
git add app/src/main/res/layout/activity_main.xml app/src/main/res/values/strings.xml app/src/androidTest/java/com/dzf/app/ui/MainActivityChromeTest.kt
git commit -m "feat: add premium home screen chrome"
```

### Task 4: Wire Home Screen Behavior, Permission Surface, And Marker Styling

**Files:**
- Create: `app/src/main/java/com/dzf/app/ui/FleetUiFormatter.kt`
- Create: `app/src/test/java/com/dzf/app/ui/FleetUiFormatterTest.kt`
- Modify: `app/src/main/java/com/dzf/app/ui/MainActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Write the failing unit test for shared status formatting**

```kotlin
package com.dzf.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FleetUiFormatterTest {

    @Test
    fun formatFleetStatus_formatsSingleDevice() {
        val text = FleetUiFormatter.formatFleetStatus(deviceCount = 1, lastRefreshAgeSeconds = 12)
        assertEquals("1 device online • refreshed 12s ago", text)
    }

    @Test
    fun formatFleetStatus_formatsPluralDevices() {
        val text = FleetUiFormatter.formatFleetStatus(deviceCount = 8, lastRefreshAgeSeconds = 4)
        assertEquals("8 devices online • refreshed 4s ago", text)
    }

    @Test
    fun formatTrackSummary_formatsDistanceWithTwoDecimals() {
        val text = FleetUiFormatter.formatTrackSummary(pointCount = 15, distanceKm = 2.345)
        assertEquals("15 points • 2.35 km", text)
    }
}
```

- [ ] **Step 2: Run the unit test to verify it fails because the helper does not exist**

Run: `./gradlew :app:testDebugUnitTest --tests com.dzf.app.ui.FleetUiFormatterTest`

Expected: FAIL with `Unresolved reference: FleetUiFormatter`.

- [ ] **Step 3: Add the formatter helper and update MainActivity to use the new visual states and marker language**

`app/src/main/java/com/dzf/app/ui/FleetUiFormatter.kt`

```kotlin
package com.dzf.app.ui

import java.util.Locale

object FleetUiFormatter {
    fun formatFleetStatus(deviceCount: Int, lastRefreshAgeSeconds: Long): String {
        val noun = if (deviceCount == 1) "device" else "devices"
        return "$deviceCount $noun online • refreshed ${lastRefreshAgeSeconds}s ago"
    }

    fun formatTrackSummary(pointCount: Int, distanceKm: Double): String {
        return String.format(Locale.US, "%d points • %.2f km", pointCount, distanceKm)
    }
}
```

Update `MainActivity.kt` with these exact changes:

```kotlin
private var lastFleetRefreshMs: Long = System.currentTimeMillis()

private fun updateFleetHeader(deviceCount: Int) {
    val ageSeconds = ((System.currentTimeMillis() - lastFleetRefreshMs) / 1000L).coerceAtLeast(0L)
    binding.deviceCountText.text = deviceCount.toString()
    binding.fleetStatusText.text = FleetUiFormatter.formatFleetStatus(deviceCount, ageSeconds)
}

private fun showMainState(title: String, body: String) {
    binding.mainStateTitleText.text = title
    binding.mainStateBodyText.text = body
    binding.mainStatePanel.visibility = View.VISIBLE
}

private fun hideMainState() {
    binding.mainStatePanel.visibility = View.GONE
}
```

Replace the success/failure body inside `loadDeviceLocations()` with:

```kotlin
result.fold(
    onSuccess = { devices ->
        lastFleetRefreshMs = System.currentTimeMillis()
        updateMarkers(devices)
        runOnUiThread {
            updateFleetHeader(devices.size)
            hideMainState()
        }
    },
    onFailure = { error ->
        runOnUiThread {
            showMainState(
                title = getString(R.string.map_load_timeout_hint),
                body = error.message ?: getString(R.string.sync_status_ready)
            )
        }
    }
)
```

Replace `updateTrackInfo()` with:

```kotlin
private fun updateTrackInfo() {
    val distance = calculateTotalDistance()
    binding.trackInfoText.text = FleetUiFormatter.formatTrackSummary(trackPoints.size, distance)
}
```

Replace `showBackgroundPermissionDialog()` with:

```kotlin
private fun showBackgroundPermissionDialog() {
    androidx.appcompat.app.AlertDialog.Builder(
        androidx.appcompat.view.ContextThemeWrapper(this, R.style.ThemeOverlay_DeviceTracker_Dialog)
    )
        .setTitle(getString(R.string.location_panel_title))
        .setMessage(getString(R.string.location_panel_body))
        .setPositiveButton("Grant") { _, _ ->
            requestBackgroundPermissionLauncher.launch(PermissionHelper.backgroundLocationPermission())
        }
        .setNegativeButton(getString(R.string.dismiss)) { _, _ ->
            maybeStartTracking()
        }
        .show()
}
```

Replace the marker icon construction with a darker, halo-based version:

```kotlin
private fun createLabeledMarkerIcon(
    name: String,
    color: Int,
    highlight: Boolean
): com.amap.api.maps.model.BitmapDescriptor {
    val width = 220
    val height = 240
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = width / 2f
    val circleCy = 82f
    val circleRadius = 34f

    if (highlight) {
        val haloPaint = Paint().apply {
            this.color = color
            alpha = 85
            isAntiAlias = true
        }
        canvas.drawCircle(cx, circleCy, circleRadius + 22f, haloPaint)
    }

    val pinPaint = Paint().apply {
        this.color = color
        isAntiAlias = true
    }
    val centerPaint = Paint().apply {
        this.color = Color.parseColor("#08111E")
        isAntiAlias = true
    }
    val borderPaint = Paint().apply {
        this.color = Color.parseColor("#DDEBFF")
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        isAntiAlias = true
    }

    canvas.drawCircle(cx, circleCy, circleRadius, pinPaint)
    canvas.drawCircle(cx, circleCy, 12f, centerPaint)
    canvas.drawCircle(cx, circleCy, circleRadius, borderPaint)

    val tail = Path().apply {
        moveTo(cx, circleCy + circleRadius - 4f)
        lineTo(cx - 16f, circleCy + circleRadius + 34f)
        lineTo(cx + 16f, circleCy + circleRadius + 34f)
        close()
    }
    canvas.drawPath(tail, pinPaint)
    canvas.drawPath(tail, borderPaint)

    val labelRect = RectF(28f, 168f, width - 28f, 214f)
    val labelBg = Paint().apply {
        this.color = Color.parseColor("#D9152B44")
        isAntiAlias = true
    }
    val labelBorder = Paint().apply {
        this.color = Color.parseColor("#55D9ECFF")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    val labelText = Paint().apply {
        this.color = Color.parseColor("#F2F7FF")
        textAlign = Paint.Align.CENTER
        textSize = 24f
        isAntiAlias = true
    }
    canvas.drawRoundRect(labelRect, 22f, 22f, labelBg)
    canvas.drawRoundRect(labelRect, 22f, 22f, labelBorder)
    canvas.drawText(if (name.length > 12) name.take(12) + "..." else name, cx, 197f, labelText)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
```

- [ ] **Step 4: Run unit tests and home instrumentation tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests com.dzf.app.ui.FleetUiFormatterTest
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dzf.app.ui.MainActivityChromeTest
```

Expected: PASS for formatter tests and home-screen chrome tests.

- [ ] **Step 5: Commit the home behavior polish**

```bash
git add app/src/main/java/com/dzf/app/ui/FleetUiFormatter.kt app/src/test/java/com/dzf/app/ui/FleetUiFormatterTest.kt app/src/main/java/com/dzf/app/ui/MainActivity.kt app/src/main/res/values/strings.xml
git commit -m "feat: wire premium home states and markers"
```

### Task 5: Redesign The Device List Screen And Card Rows

**Files:**
- Modify: `app/src/main/res/layout/activity_device_list.xml`
- Modify: `app/src/main/res/layout/item_device.xml`
- Modify: `app/src/main/java/com/dzf/app/ui/DeviceListActivity.kt`
- Modify: `app/src/main/java/com/dzf/app/ui/DeviceListAdapter.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/androidTest/java/com/dzf/app/ui/DeviceListActivityChromeTest.kt`

- [ ] **Step 1: Write the failing instrumentation test for the list chrome**

```kotlin
package com.dzf.app.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dzf.app.R
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceListActivityChromeTest {

    @Test
    fun deviceListScreen_rendersNewStatePanelAndRecycler() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, DeviceListActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        ActivityScenario.launch<DeviceListActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById(R.id.deviceRecyclerView))
                assertNotNull(activity.findViewById(R.id.listStatePanel))
                assertNotNull(activity.findViewById(R.id.closeButton))
            }
        }
    }
}
```

- [ ] **Step 2: Run the test and verify it fails because `listStatePanel` does not exist**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dzf.app.ui.DeviceListActivityChromeTest`

Expected: FAIL because the new ids are not yet present.

- [ ] **Step 3: Replace the list page and row layout with dark glass telemetry cards**

`app/src/main/res/layout/activity_device_list.xml`

```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/surface_primary">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:paddingStart="20dp"
        android:paddingTop="16dp"
        android:paddingEnd="20dp">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            style="@style/Widget.DeviceTracker.Toolbar"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            app:navigationIcon="@drawable/ic_fleet_close"
            app:title="@string/device_list_title"
            app:subtitle="@string/live_location_network" />

        <LinearLayout
            android:id="@+id/listStatePanel"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="14dp"
            android:background="@drawable/bg_state_panel"
            android:orientation="vertical"
            android:padding="18dp"
            android:visibility="gone">

            <TextView
                android:id="@+id/listStateTitleText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/text_primary"
                android:textSize="16sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/listStateBodyText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:textColor="@color/text_secondary"
                android:textSize="14sp" />
        </LinearLayout>

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/deviceRecyclerView"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:clipToPadding="false"
            android:paddingTop="18dp"
            android:paddingBottom="24dp" />
    </LinearLayout>

    <ImageButton
        android:id="@+id/closeButton"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_gravity="top|end"
        android:layout_marginTop="18dp"
        android:layout_marginEnd="20dp"
        android:background="@drawable/bg_button_glass"
        android:contentDescription="@string/close"
        android:padding="14dp"
        android:src="@drawable/ic_fleet_close"
        app:tint="@color/text_primary" />
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

`app/src/main/res/layout/item_device.xml`

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="12dp"
    android:background="@drawable/bg_panel_glass_secondary"
    android:orientation="vertical"
    android:padding="18dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center_vertical"
        android:orientation="horizontal">

        <TextView
            android:id="@+id/deviceNameText"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textColor="@color/text_primary"
            android:textSize="18sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/deviceStatusChip"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:paddingStart="10dp"
            android:paddingTop="6dp"
            android:paddingEnd="10dp"
            android:paddingBottom="6dp"
            android:textColor="@color/text_primary"
            android:textSize="12sp" />
    </LinearLayout>

    <TextView
        android:id="@+id/deviceIdText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="6dp"
        android:textColor="@color/text_muted"
        android:textSize="12sp" />

    <TextView
        android:id="@+id/deviceStatusText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="10dp"
        android:textColor="@color/text_secondary"
        android:textSize="14sp" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="14dp"
        android:gravity="end"
        android:orientation="horizontal">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/exportButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginEnd="8dp"
            android:insetTop="0dp"
            android:insetBottom="0dp"
            android:text="@string/export_today_timeline"
            android:textAllCaps="false"
            android:textColor="@color/text_primary"
            app:backgroundTint="@color/surface_panel"
            app:icon="@drawable/ic_fleet_export"
            app:iconTint="@color/accent_icy"
            app:strokeColor="@color/stroke_subtle" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/trackButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:insetTop="0dp"
            android:insetBottom="0dp"
            android:text="@string/view_track"
            android:textAllCaps="false"
            android:textColor="@color/surface_primary"
            app:backgroundTint="@color/accent_icy"
            app:icon="@drawable/ic_fleet_track"
            app:iconTint="@color/surface_primary" />
    </LinearLayout>
</LinearLayout>
```

Update the binding logic in `DeviceListAdapter.kt`:

```kotlin
private val statusChip: TextView = itemView.findViewById(R.id.deviceStatusChip)

val state = if (item.isOnline) itemView.context.getString(R.string.online) else itemView.context.getString(R.string.offline)
val time = timeFormat.format(Date(item.timestamp))
statusChip.text = state
statusChip.setBackgroundResource(if (item.isOnline) R.drawable.bg_chip_status_online else R.drawable.bg_chip_status_offline)
nameText.text = item.deviceName.ifBlank { itemView.context.getString(R.string.unknown_device) }
idText.text = "ID: ${item.deviceId}"
statusText.text = itemView.context.getString(R.string.last_seen, time)
```

Update `DeviceListActivity.kt` success/failure handling:

```kotlin
private fun showListState(title: String, body: String) {
    binding.listStateTitleText.text = title
    binding.listStateBodyText.text = body
    binding.listStatePanel.visibility = android.view.View.VISIBLE
}

private fun hideListState() {
    binding.listStatePanel.visibility = android.view.View.GONE
}
```

```kotlin
result.fold(
    onSuccess = { devices ->
        hideListState()
        binding.deviceRecyclerView.adapter = DeviceListAdapter(
            items = devices,
            onTrackClick = { device ->
                val intent = Intent(this@DeviceListActivity, DeviceTrackActivity::class.java)
                intent.putExtra(DeviceTrackActivity.EXTRA_DEVICE_ID, device.deviceId)
                intent.putExtra(DeviceTrackActivity.EXTRA_DEVICE_NAME, device.deviceName)
                startActivity(intent)
            },
            onExportClick = { device -> exportTimeline(device) }
        )
    },
    onFailure = { error ->
        showListState(
            getString(R.string.device_list_title),
            getString(R.string.export_failed, error.message ?: "unknown")
        )
    }
)
```

- [ ] **Step 4: Run the list instrumentation test and assemble the app**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dzf.app.ui.DeviceListActivityChromeTest
./gradlew :app:assembleDebug
```

Expected: PASS for `DeviceListActivityChromeTest` and `BUILD SUCCESSFUL` for assemble.

- [ ] **Step 5: Commit the list redesign**

```bash
git add app/src/main/res/layout/activity_device_list.xml app/src/main/res/layout/item_device.xml app/src/main/java/com/dzf/app/ui/DeviceListActivity.kt app/src/main/java/com/dzf/app/ui/DeviceListAdapter.kt app/src/main/res/values/strings.xml app/src/androidTest/java/com/dzf/app/ui/DeviceListActivityChromeTest.kt
git commit -m "feat: redesign device list screen"
```

### Task 6: Redesign The Track Screen And Add In-Layout Empty State

**Files:**
- Modify: `app/src/main/res/layout/activity_device_track.xml`
- Modify: `app/src/main/java/com/dzf/app/ui/DeviceTrackActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/androidTest/java/com/dzf/app/ui/DeviceTrackActivityChromeTest.kt`

- [ ] **Step 1: Write the failing instrumentation test for the track screen chrome**

```kotlin
package com.dzf.app.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dzf.app.R
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceTrackActivityChromeTest {

    @Test
    fun trackScreen_rendersSummaryAndStatePanels() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, DeviceTrackActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(DeviceTrackActivity.EXTRA_DEVICE_ID, "device-1")
            .putExtra(DeviceTrackActivity.EXTRA_DEVICE_NAME, "Field Unit")

        ActivityScenario.launch<DeviceTrackActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.findViewById(R.id.trackSummaryCard))
                assertNotNull(activity.findViewById(R.id.trackStatePanel))
                assertNotNull(activity.findViewById(R.id.closeButton))
            }
        }
    }
}
```

- [ ] **Step 2: Run the test and verify it fails because the new ids do not exist**

Run: `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dzf.app.ui.DeviceTrackActivityChromeTest`

Expected: FAIL because `trackSummaryCard` and `trackStatePanel` are missing.

- [ ] **Step 3: Replace the track layout and wire the empty/error panel behavior**

`app/src/main/res/layout/activity_device_track.xml`

```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/surface_primary">

    <com.amap.api.maps.MapView
        android:id="@+id/mapView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <View
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#2206111E" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="top"
        android:orientation="vertical"
        android:paddingStart="20dp"
        android:paddingTop="16dp"
        android:paddingEnd="20dp">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            style="@style/Widget.DeviceTracker.Toolbar"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            app:navigationIcon="@drawable/ic_fleet_close"
            app:title="@string/track_map_title" />

        <LinearLayout
            android:id="@+id/trackSummaryCard"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="14dp"
            android:background="@drawable/bg_panel_glass_primary"
            android:orientation="vertical"
            android:padding="18dp">

            <TextView
                android:id="@+id/trackSummaryTitleText"
                style="@style/TextAppearance.DeviceTracker.Overline"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/track_map_title" />

            <TextView
                android:id="@+id/trackSummaryBodyText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:textColor="@color/text_primary"
                android:textSize="18sp"
                android:textStyle="bold" />
        </LinearLayout>

        <LinearLayout
            android:id="@+id/trackStatePanel"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:background="@drawable/bg_state_panel"
            android:orientation="vertical"
            android:padding="18dp"
            android:visibility="gone">

            <TextView
                android:id="@+id/trackStateTitleText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/text_primary"
                android:textSize="16sp"
                android:textStyle="bold" />

            <TextView
                android:id="@+id/trackStateBodyText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:textColor="@color/text_secondary"
                android:textSize="14sp" />
        </LinearLayout>
    </LinearLayout>

    <ImageButton
        android:id="@+id/closeButton"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:layout_gravity="top|end"
        android:layout_marginTop="18dp"
        android:layout_marginEnd="20dp"
        android:background="@drawable/bg_button_glass"
        android:contentDescription="@string/close"
        android:padding="14dp"
        android:src="@drawable/ic_fleet_close"
        app:tint="@color/text_primary" />
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

Update `DeviceTrackActivity.kt` with these helpers:

```kotlin
private fun showTrackState(title: String, body: String) {
    binding.trackStateTitleText.text = title
    binding.trackStateBodyText.text = body
    binding.trackStatePanel.visibility = android.view.View.VISIBLE
}

private fun hideTrackState() {
    binding.trackStatePanel.visibility = android.view.View.GONE
}
```

Replace the `loadTrack()` result handling block with:

```kotlin
result.fold(
    onSuccess = { track ->
        binding.trackSummaryBodyText.text = if (deviceName.isBlank()) {
            getString(R.string.track_map_title)
        } else {
            deviceName
        }

        if (track.isEmpty()) {
            showTrackState(
                getString(R.string.export_empty_timeline),
                getString(R.string.location_panel_body)
            )
            return@fold
        }

        hideTrackState()
        val points = track.map { LatLng(it.latitude, it.longitude) }
        aMap.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(Color.parseColor("#59A7FF"))
                .width(12f)
                .geodesic(true)
        )

        val start = points.first()
        val current = points.last()
        aMap.addMarker(
            MarkerOptions()
                .position(start)
                .title("Start")
                .snippet("Track starting point")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
        aMap.addMarker(
            MarkerOptions()
                .position(current)
                .title("Current")
                .snippet("Latest location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { boundsBuilder.include(it) }
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120))
    },
    onFailure = { error ->
        showTrackState(
            getString(R.string.track_map_title),
            error.message ?: getString(R.string.export_failed, "unknown")
        )
    }
)
```

- [ ] **Step 4: Run the track instrumentation test and assemble the app**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dzf.app.ui.DeviceTrackActivityChromeTest
./gradlew :app:assembleDebug
```

Expected: PASS for `DeviceTrackActivityChromeTest` and `BUILD SUCCESSFUL` for assemble.

- [ ] **Step 5: Commit the track screen redesign**

```bash
git add app/src/main/res/layout/activity_device_track.xml app/src/main/java/com/dzf/app/ui/DeviceTrackActivity.kt app/src/main/res/values/strings.xml app/src/androidTest/java/com/dzf/app/ui/DeviceTrackActivityChromeTest.kt
git commit -m "feat: redesign track screen and empty state"
```

### Task 7: Run Full Verification And Fix Integration Gaps

**Files:**
- Modify: `app/src/main/java/com/dzf/app/ui/MainActivity.kt` as needed
- Modify: `app/src/main/java/com/dzf/app/ui/DeviceListActivity.kt` as needed
- Modify: `app/src/main/java/com/dzf/app/ui/DeviceTrackActivity.kt` as needed
- Modify: `app/src/main/res/layout/*.xml` as needed

- [ ] **Step 1: Run the full local verification suite and capture all failures**

Run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

Expected: one or more failures if any ids, imports, or resource references drifted during integration.

- [ ] **Step 2: Fix any compile or binding issues with these exact cleanup checks**

Review and fix these hotspots if the suite fails:

```kotlin
// MainActivity.kt cleanup checklist
// 1. Remove old references to MaterialCardView-specific APIs if the views are now LinearLayouts.
// 2. Keep binding ids aligned with activity_main.xml: fleetOverviewCard, networkStatusText, fleetStatusText, mainStatePanel.
// 3. Keep trackFab visibility logic valid for ImageButton.

// DeviceListActivity.kt cleanup checklist
// 1. Ensure toolbar/closeButton wiring still calls finish().
// 2. Ensure listStatePanel ids match the XML exactly.

// DeviceTrackActivity.kt cleanup checklist
// 1. Keep closeButton and toolbar navigation both finishing the activity.
// 2. Ensure trackStatePanel and trackSummaryBodyText ids exist in binding.
```

If any string text reads awkwardly after integration, make these exact replacements in `strings.xml`:

```xml
<string name="track_empty_body">No route data has been uploaded for this device today yet.</string>
<string name="list_error_body">We could not refresh the device roster. Check network and try again.</string>
<string name="map_error_body">The live map overlay could not refresh fleet data.</string>
```

Then use them in the activities instead of generic fallback text.

- [ ] **Step 3: Re-run the full suite until all verification is green**

Run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

Expected: all commands succeed with `BUILD SUCCESSFUL` and passing tests.

- [ ] **Step 4: Smoke-check the launcher and premium chrome on device**

Run:

```bash
adb shell am start -n com.dzf.app/.ui.MainActivity
```

Expected: app opens with the dark map overlay, premium buttons, and new launcher icon visible on the device home screen/app drawer.

- [ ] **Step 5: Commit the integrated redesign**

```bash
git add app/src/main/java/com/dzf/app/ui app/src/main/res/layout app/src/main/res/values app/src/main/res/drawable app/src/test/java/com/dzf/app/ui app/src/androidTest/java/com/dzf/app/ui
git commit -m "feat: ship DeviceTracker premium visual redesign"
```

---

## Self-Review Checklist

### Spec Coverage

1. Midnight Control Glass visual direction: covered by Tasks 1 through 6.
2. Home map console memory point: covered by Tasks 3 and 4.
3. Device list redesign: covered by Task 5.
4. Track screen redesign and in-layout empty state: covered by Task 6.
5. Permission guidance polish: covered by Task 4.
6. Icon system and launcher redesign: covered by Task 2.
7. Marker redesign: covered by Task 4.
8. Full verification and integration pass: covered by Task 7.

### Placeholder Scan

No `TODO`, `TBD`, or "similar to above" instructions remain. Each task names exact files, commands, and code snippets.

### Type Consistency

1. Shared ids are consistent across plan steps: `mainStatePanel`, `listStatePanel`, `trackStatePanel`.
2. Shared helper name stays `FleetUiFormatter` across tests and implementation.
3. Shared theme name stays `Theme.DeviceTracker` and `ThemeOverlay.DeviceTracker.Dialog` across resources and code.
