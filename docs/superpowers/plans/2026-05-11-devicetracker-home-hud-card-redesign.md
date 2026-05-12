# DeviceTracker Home HUD Card Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the homepage `在线设备` and `轨迹记录` cards into a narrower left-side vertical HUD stack that preserves readability while giving more space back to the map.

**Architecture:** Keep the current homepage map screen and business logic intact, but restructure the top overlay in `activity_main.xml` so the two cards become a compact vertical intelligence strip under the title block. Support the compact layout with only minimal style and spacing changes, reusing existing string and binding logic wherever possible.

**Tech Stack:** Android Views, XML layouts, ViewBinding, Kotlin, Material3 text styles, drawable-based glass panels

---

## File Map

- Modify: `app/src/main/res/layout/activity_main.xml`
  - Responsibility: Restructure homepage overlay from horizontal dual-card layout to a compact vertical left HUD stack.
- Modify: `app/src/main/res/values/dimens.xml`
  - Responsibility: Add any small new spacing or size tokens needed for the compressed HUD layout.
- Modify: `app/src/main/res/values/styles.xml`
  - Responsibility: Add compact text appearances if the existing metric style is too large for the new HUD cards.
- Modify: `app/src/main/res/values/strings.xml`
  - Responsibility: Introduce a shorter compact track summary label only if layout density requires it.
- Verify: `app/src/main/java/com/dzf/app/ui/MainActivity.kt`
  - Responsibility: Confirm existing binding code still works with the updated view IDs and content model, without logic changes.

### Task 1: Restructure Homepage Card Layout

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml:57-137`

- [ ] **Step 1: Define the failing UI condition before editing**

The current homepage card group is too wide, too tall, and inconsistent in perceived height because it uses a large flexible fleet card plus a separate narrow track card in a horizontal row.

The replacement layout must satisfy all of these rules:

```text
- cards stack vertically on the left side
- total strip width targets about 128dp to 138dp
- online card remains visually primary but compact
- track card remains shorter and denser, with short summary text
- the map center and top-right become more visible than before
```

- [ ] **Step 2: Replace the horizontal card row with a vertical HUD stack**

Update the card section in `activity_main.xml` from the current horizontal row into a single narrow vertical container placed under the title block.

The target structure should look like this:

```xml
<LinearLayout
    android:id="@+id/homeHudStrip"
    android:layout_width="136dp"
    android:layout_height="wrap_content"
    android:layout_marginTop="@dimen/space_md"
    android:orientation="vertical">

    <LinearLayout
        android:id="@+id/deviceCountCard"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@drawable/bg_panel_glass_primary"
        android:minHeight="76dp"
        android:orientation="vertical"
        android:paddingStart="@dimen/space_md"
        android:paddingTop="@dimen/space_md"
        android:paddingEnd="@dimen/space_md"
        android:paddingBottom="@dimen/space_sm">

        <LinearLayout
            android:id="@+id/fleetOverviewCard"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <TextView
                android:id="@+id/fleetLabelText"
                style="@style/TextAppearance.DeviceTracker.Overline"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/fleet_online" />

            <TextView
                android:id="@+id/deviceCountText"
                style="@style/TextAppearance.DeviceTracker.MetricCompact"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_xs"
                android:text="0" />

            <TextView
                android:id="@+id/fleetStatusText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_xs"
                android:maxLines="1"
                android:text="@string/sync_status_ready"
                android:textColor="@color/text_secondary"
                android:textSize="11sp" />
        </LinearLayout>
    </LinearLayout>

    <LinearLayout
        android:id="@+id/trackCard"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:background="@drawable/bg_panel_glass_secondary"
        android:minHeight="68dp"
        android:orientation="vertical"
        android:paddingStart="@dimen/space_md"
        android:paddingTop="@dimen/space_sm"
        android:paddingEnd="@dimen/space_md"
        android:paddingBottom="@dimen/space_sm">

        <LinearLayout
            android:id="@+id/trackStatusCard"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <TextView
                style="@style/TextAppearance.DeviceTracker.Overline"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/track_recording" />

            <TextView
                android:id="@+id/trackInfoText"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_xs"
                android:maxLines="1"
                android:text="@string/track_status_idle"
                android:textColor="@color/text_primary"
                android:textSize="12sp"
                android:textStyle="bold" />
        </LinearLayout>
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 3: Keep the title block and state panel behavior intact**

Do not change these surrounding structures except for any margin tuning required to support the new compact stack:

```text
- app title text
- networkStatusText
- mainStatePanel visibility and placement semantics
- trackFab and myLocationFab behavior
```

- [ ] **Step 4: Verify the layout still uses the same view IDs required by binding code**

The following IDs must remain present because `MainActivity.kt` already uses them:

```text
deviceCountCard
fleetOverviewCard
fleetLabelText
deviceCountText
fleetStatusText
trackCard
trackStatusCard
trackInfoText
```

Expected: no `MainActivity.kt` logic changes are required just to keep binding valid.

### Task 2: Add Compact HUD Typography and Spacing

**Files:**
- Modify: `app/src/main/res/values/styles.xml:3-25`
- Modify: `app/src/main/res/values/dimens.xml:3-12`

- [ ] **Step 1: Add a compact metric text style for the fleet count**

The existing `TextAppearance.DeviceTracker.Metric` uses `DisplaySmall`, which is too large for the new HUD strip. Add a smaller dedicated style instead of reusing the large one.

Add this style:

```xml
<style name="TextAppearance.DeviceTracker.MetricCompact" parent="TextAppearance.Material3.HeadlineMedium">
    <item name="android:textStyle">bold</item>
    <item name="android:textColor">@color/text_primary</item>
    <item name="android:letterSpacing">0.01</item>
</style>
```

- [ ] **Step 2: Add only minimal size tokens if the XML needs them**

If hardcoded dimensions are avoidable, introduce compact HUD tokens in `dimens.xml` similar to:

```xml
<dimen name="hud_strip_width">136dp</dimen>
<dimen name="hud_card_gap">8dp</dimen>
<dimen name="hud_card_primary_min_height">76dp</dimen>
<dimen name="hud_card_secondary_min_height">68dp</dimen>
```

Use them only if they improve clarity in `activity_main.xml`. Do not create extra tokens that are used once and add noise.

- [ ] **Step 3: Keep the visual language in the existing theme family**

Do not introduce a new theme direction. The compact HUD should still read as the same glass-control system by keeping:

```text
- existing panel drawables
- current text colors
- current corner radius family
- subtle primary/secondary card distinction
```

### Task 3: Tighten Copy Only If the Compact Layout Requires It

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Verify: `app/src/main/java/com/dzf/app/ui/MainActivity.kt:124-129`

- [ ] **Step 1: Check whether the existing bound strings fit the new compact cards**

Current binding sources include:

```text
sync_status_ready
track_status_idle
track_summary_template
fleet_online_count
fleet_status_refreshed
```

If these fit cleanly within the narrow HUD cards, do not change strings just for style preference.

- [ ] **Step 2: If the track card still wraps too often, add one short compact summary string**

Only if needed for the narrow track card, add a compact helper string such as:

```xml
<string name="track_summary_compact">%1$d 点 / %2$.1f km</string>
```

Then update the binding source in `MainActivity.kt` from:

```kotlin
trackSummaryTemplate = getString(R.string.track_summary_template)
```

to:

```kotlin
trackSummaryTemplate = getString(R.string.track_summary_compact)
```

Do not change logic beyond selecting the shorter display template.

- [ ] **Step 3: Keep the fleet secondary line to one line if possible**

If the freshness text is too long in the compact width, prefer XML constraints such as `maxLines="1"` and smaller text before introducing more string variants.

### Task 4: Verify the Homepage HUD Redesign

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/styles.xml`
- Modify: `app/src/main/res/values/dimens.xml`
- Optionally modify: `app/src/main/res/values/strings.xml`
- Optionally modify: `app/src/main/java/com/dzf/app/ui/MainActivity.kt`

- [ ] **Step 1: Build Android resources after the XML/style changes**

Run:

```bash
"/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe" -Command "Set-Location 'D:\Git\Demo\DeviceTracker'; & 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' '-Dorg.gradle.appname=gradlew' '-classpath' 'gradle\wrapper\gradle-wrapper.jar' 'org.gradle.wrapper.GradleWrapperMain' ':app:processDebugResources'"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Run a full debug assemble to catch binding or XML integration issues**

Run:

```bash
"/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe" -Command "Set-Location 'D:\Git\Demo\DeviceTracker'; & 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' '-Dorg.gradle.appname=gradlew' '-classpath' 'gradle\wrapper\gradle-wrapper.jar' 'org.gradle.wrapper.GradleWrapperMain' ':app:assembleDebug'"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Review the final diff to ensure the work stayed tightly scoped**

Run:

```bash
git diff -- app/src/main/res/layout/activity_main.xml app/src/main/res/values/styles.xml app/src/main/res/values/dimens.xml app/src/main/res/values/strings.xml app/src/main/java/com/dzf/app/ui/MainActivity.kt
```

Expected:

```text
- homepage overlay cards are now a narrow left vertical strip
- no unrelated homepage logic changes
- only minimal style/spacing/string adjustments were introduced
```

- [ ] **Step 4: Manually inspect against the spec acceptance criteria**

Confirm from the resulting XML and rendered structure that:

```text
- cards no longer dominate the map viewport
- the left strip width is within the intended compact range
- card heights feel intentionally aligned and controlled
- the map has regained clear visual priority
```
