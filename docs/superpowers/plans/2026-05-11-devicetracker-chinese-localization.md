# DeviceTracker Chinese Localization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert the redesigned DeviceTracker Android UI to polished Simplified Chinese, including resource copy and remaining hardcoded user-visible text, without changing app behavior.

**Architecture:** Keep the existing Android XML plus Activity architecture unchanged and treat `app/src/main/res/values/strings.xml` as the primary source of user-visible copy. Replace the remaining hardcoded English literals in `MainActivity.kt` and `DeviceTrackActivity.kt` with resource-backed strings so all visible UI text is centralized and consistent.

**Tech Stack:** Android Views, Kotlin, XML string resources, Android plurals, Gradle 8.7 wrapper, AGP resource processing

---

## File Map

- Modify: `app/src/main/res/values/strings.xml`
  - Responsibility: Main UI copy, button labels, empty states, error states, service text, and newly resourceized `Toast` and marker strings.
- Modify: `app/src/main/res/values/plurals.xml`
  - Responsibility: Fleet online quantity formatting.
- Modify: `app/src/main/java/com/dzf/app/ui/MainActivity.kt`
  - Responsibility: Replace hardcoded permission and location-status `Toast` messages with `getString(...)` resource lookups.
- Modify: `app/src/main/java/com/dzf/app/ui/DeviceTrackActivity.kt`
  - Responsibility: Replace hardcoded invalid-device and marker title/snippet text with resource lookups.
- Verify: `docs/superpowers/specs/2026-05-11-devicetracker-chinese-localization-design.md`
  - Responsibility: Approved design reference.

### Task 1: Translate and Expand String Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml:2-67`
- Modify: `app/src/main/res/values/plurals.xml:1-6`

- [ ] **Step 1: Write the failing resource compilation check expectation**

The localization change must keep Android resource formatting valid while converting the visible UI copy to Chinese. The main risk is breaking placeholder syntax or escaping.

Target strings to update include the existing English user-visible values and these new keys for code-side literals:

```xml
<string name="toast_location_permission_required">应用需要位置权限才能正常使用</string>
<string name="toast_background_location_denied">后台定位未授权，应用在后台时可能暂停上传位置</string>
<string name="toast_waiting_for_location">正在等待定位更新</string>
<string name="toast_failed_to_get_location">获取定位失败</string>
<string name="invalid_device_id">设备标识无效</string>
<string name="track_marker_start_title">起点</string>
<string name="track_marker_start_snippet">轨迹起始位置</string>
<string name="track_marker_current_title">当前位置</string>
<string name="track_marker_current_snippet">最新上报位置</string>
```

- [ ] **Step 2: Run the resource task before editing to establish the baseline**

Run:

```bash
"/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe" -Command "Set-Location 'D:\Git\Demo\DeviceTracker'; & 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' '-Dorg.gradle.appname=gradlew' '-classpath' 'gradle\wrapper\gradle-wrapper.jar' 'org.gradle.wrapper.GradleWrapperMain' ':app:processDebugResources'"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Update `strings.xml` to polished Simplified Chinese**

Translate the visible strings in place, keeping the existing keys and placeholder semantics. Use concise product copy similar to this shape:

```xml
<resources>
    <string name="app_name">设备追踪</string>
    <string name="maps_api_key">YOUR_MAPS_API_KEY_HERE</string>
    <string name="permission_location_rationale">需要位置权限才能在地图上显示本机位置并追踪其他设备。</string>
    <string name="permission_background_location_rationale">需要后台定位权限，才能在应用退到后台后持续更新当前位置。</string>
    <string name="permission_notification_rationale">需要通知权限以启用定位追踪服务。</string>
    <string name="location_service_channel_name">位置追踪</string>
    <string name="location_service_channel_description">持续追踪设备当前位置</string>
    <string name="location_alert_channel_name">位置提醒</string>
    <string name="location_alert_channel_description">定位失败与重要提醒通知</string>
    <string name="location_service_title">正在追踪你的位置</string>
    <string name="location_service_text">当前位置正在同步给其他设备</string>
    <string name="location_failed_title">位置更新失败</string>
    <string name="location_failed_text">无法获取有效位置（%1$d）：%2$s</string>
    <string name="my_location">我的位置</string>
    <string name="device_count">设备数：%1$d</string>
    <string name="online">在线</string>
    <string name="offline">离线</string>
    <string name="last_seen">最后在线：%1$s</string>
    <string name="this_device">本机设备</string>
    <string name="unknown_device">未知设备</string>
    <string name="track_recording">轨迹记录</string>
    <string name="start_tracking">开始追踪</string>
    <string name="stop_tracking">停止追踪</string>
    <string name="track_info">轨迹点：%1$d | 距离：%2$.2f 公里</string>
    <string name="track_summary_template">%1$d 个轨迹点 • %2$.2f 公里</string>
    <string name="tracking_started">已开始追踪</string>
    <string name="tracking_stopped">已停止追踪</string>
    <string name="device_list_title">设备列表</string>
    <string name="device_id_value">ID：%1$s</string>
    <string name="device_list_empty_title">暂无在线设备</string>
    <string name="device_list_empty_body">当实时定位同步到设备网络后，设备会显示在这里。</string>
    <string name="device_list_load_failed_title">设备列表暂时不可用</string>
    <string name="device_list_load_failed_body">暂时无法加载最新设备列表，原因：%1$s</string>
    <string name="view_track">查看轨迹</string>
    <string name="export_today_timeline">导出时间线</string>
    <string name="track_map_title">轨迹详情</string>
    <string name="track_summary_label">轨迹回放</string>
    <string name="track_summary_today">今日轨迹</string>
    <string name="track_summary_no_route">今天还没有采集到轨迹</string>
    <string name="track_summary_points">已采集 %1$d 个轨迹点</string>
    <string name="track_summary_distance">今日轨迹距离 %1$.2f 公里</string>
    <string name="track_empty_title">今日暂无轨迹数据</string>
    <string name="track_empty_body">该设备暂未上报轨迹点，请返回实时地图或等待下一次位置同步后再试。</string>
    <string name="track_error_title">轨迹回放暂不可用</string>
    <string name="track_error_body">暂时无法加载这段轨迹，原因：%1$s</string>
    <string name="track_return_action">返回实时地图</string>
    <string name="track_retry_action">重新加载</string>
    <string name="track_status_idle">待命中</string>
    <string name="close">关闭</string>
    <string name="export_failed">导出失败：%1$s</string>
    <string name="export_empty_timeline">今日暂无可导出的时间线数据</string>
    <string name="export_file_prefix">时间线</string>
    <string name="share_timeline_title">分享时间线</string>
    <string name="map_load_timeout_title">地图同步延迟</string>
    <string name="map_refresh_failed_title">设备网络刷新失败</string>
    <string name="map_load_timeout_hint">地图加载时间过长，请检查高德 Key 和网络连接。</string>
    <string name="map_error_body">实时地图图层暂时无法刷新设备数据。</string>
    <string name="fleet_online">在线设备</string>
    <string name="fleet_status_refreshed">%1$s • %2$d 秒前更新</string>
    <string name="live_location_network">实时位置网络</string>
    <string name="sync_status_ready">实时同步已就绪</string>
    <string name="location_panel_title">需要位置访问权限</string>
    <string name="location_panel_body">请开启后台定位权限，以便应用不在前台时，此设备仍能继续显示在实时地图中。</string>
    <string name="grant">去授权</string>
    <string name="dismiss">暂不处理</string>
    <string name="unknown_error">未知错误</string>
    <string name="toast_location_permission_required">应用需要位置权限才能正常使用</string>
    <string name="toast_background_location_denied">后台定位未授权，应用在后台时可能暂停上传位置</string>
    <string name="toast_waiting_for_location">正在等待定位更新</string>
    <string name="toast_failed_to_get_location">获取定位失败</string>
    <string name="invalid_device_id">设备标识无效</string>
    <string name="track_marker_start_title">起点</string>
    <string name="track_marker_start_snippet">轨迹起始位置</string>
    <string name="track_marker_current_title">当前位置</string>
    <string name="track_marker_current_snippet">最新上报位置</string>
</resources>
```

- [ ] **Step 4: Update `plurals.xml` to natural Chinese quantity copy**

Replace the current English items with a Chinese quantity string while preserving the plural resource key:

```xml
<resources>
    <plurals name="fleet_online_count">
        <item quantity="one">%1$d 台设备在线</item>
        <item quantity="other">%1$d 台设备在线</item>
    </plurals>
</resources>
```

- [ ] **Step 5: Re-run resource compilation to verify the translated resources are valid**

Run:

```bash
"/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe" -Command "Set-Location 'D:\Git\Demo\DeviceTracker'; & 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' '-Dorg.gradle.appname=gradlew' '-classpath' 'gradle\wrapper\gradle-wrapper.jar' 'org.gradle.wrapper.GradleWrapperMain' ':app:processDebugResources'"
```

Expected: `BUILD SUCCESSFUL`

### Task 2: Replace Hardcoded Toast Strings in `MainActivity`

**Files:**
- Modify: `app/src/main/java/com/dzf/app/ui/MainActivity.kt:100-114`
- Modify: `app/src/main/java/com/dzf/app/ui/MainActivity.kt:656-663`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Identify the exact literals being replaced**

These user-visible strings must stop being hardcoded:

```kotlin
"Location permission is required for this app to work"
"Background location denied. Upload may pause in background."
"Waiting for location..."
"Failed to get location"
```

- [ ] **Step 2: Replace each `Toast` literal with `getString(...)`**

Update the affected blocks to this shape:

```kotlin
Toast.makeText(this, getString(R.string.toast_location_permission_required), Toast.LENGTH_LONG).show()
```

```kotlin
Toast.makeText(
    this,
    getString(R.string.toast_background_location_denied),
    Toast.LENGTH_LONG
).show()
```

```kotlin
Toast.makeText(this@MainActivity, getString(R.string.toast_waiting_for_location), Toast.LENGTH_SHORT).show()
```

```kotlin
Toast.makeText(this@MainActivity, getString(R.string.toast_failed_to_get_location), Toast.LENGTH_SHORT).show()
```

- [ ] **Step 3: Keep behavior unchanged while cleaning only the text source**

Do not alter any permission flow, coroutine behavior, or map camera behavior. The only intended code change is resource lookup substitution around the existing `Toast.makeText(...)` calls.

- [ ] **Step 4: Grep the file to verify those English literals are gone**

Run:

```bash
rg -n "Location permission is required for this app to work|Background location denied\. Upload may pause in background\.|Waiting for location\.\.\.|Failed to get location" app/src/main/java/com/dzf/app/ui/MainActivity.kt
```

Expected: no matches

### Task 3: Replace Hardcoded Track-Screen Strings in `DeviceTrackActivity`

**Files:**
- Modify: `app/src/main/java/com/dzf/app/ui/DeviceTrackActivity.kt:77-84`
- Modify: `app/src/main/java/com/dzf/app/ui/DeviceTrackActivity.kt:128-142`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Identify the exact track-screen literals being replaced**

These user-visible strings must stop being hardcoded:

```kotlin
"Invalid device id"
"Start"
"Track starting point"
"Current"
"Latest location"
```

- [ ] **Step 2: Replace the invalid-device `Toast` with a string resource**

Update the invalid-device branch to this shape:

```kotlin
Toast.makeText(this, getString(R.string.invalid_device_id), Toast.LENGTH_SHORT).show()
```

- [ ] **Step 3: Replace marker titles and snippets with string resources**

Update the marker blocks to this shape:

```kotlin
aMap.addMarker(
    MarkerOptions()
        .position(start)
        .title(getString(R.string.track_marker_start_title))
        .snippet(getString(R.string.track_marker_start_snippet))
        .anchor(startMarkerSpec().anchorU, startMarkerSpec().anchorV)
        .icon(createMarkerIcon(startMarkerSpec()))
)

aMap.addMarker(
    MarkerOptions()
        .position(current)
        .title(getString(R.string.track_marker_current_title))
        .snippet(getString(R.string.track_marker_current_snippet))
        .anchor(currentMarkerSpec().anchorU, currentMarkerSpec().anchorV)
        .icon(createMarkerIcon(currentMarkerSpec()))
)
```

- [ ] **Step 4: Grep the file to verify those English literals are gone**

Run:

```bash
rg -n "Invalid device id|Start|Track starting point|Current|Latest location" app/src/main/java/com/dzf/app/ui/DeviceTrackActivity.kt
```

Expected: no matches

### Task 4: Verify the Full Localization Pass

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/plurals.xml`
- Modify: `app/src/main/java/com/dzf/app/ui/MainActivity.kt`
- Modify: `app/src/main/java/com/dzf/app/ui/DeviceTrackActivity.kt`

- [ ] **Step 1: Run Android resource processing after all code updates**

Run:

```bash
"/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe" -Command "Set-Location 'D:\Git\Demo\DeviceTracker'; & 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' '-Dorg.gradle.appname=gradlew' '-classpath' 'gradle\wrapper\gradle-wrapper.jar' 'org.gradle.wrapper.GradleWrapperMain' ':app:processDebugResources'"
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Run a broader debug build if the environment allows it**

Run:

```bash
"/mnt/c/Windows/System32/WindowsPowerShell/v1.0/powershell.exe" -Command "Set-Location 'D:\Git\Demo\DeviceTracker'; & 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe' '-Dorg.gradle.appname=gradlew' '-classpath' 'gradle\wrapper\gradle-wrapper.jar' 'org.gradle.wrapper.GradleWrapperMain' ':app:assembleDebug'"
```

Expected: `BUILD SUCCESSFUL`

If this fails for a non-localization reason, keep the successful `processDebugResources` result as the minimum acceptance bar and document the blocking task output.

- [ ] **Step 3: Search for likely remaining user-visible English copy in the touched app surface**

Run:

```bash
rg -n "Location permission is required for this app to work|Background location denied\. Upload may pause in background\.|Waiting for location\.\.\.|Failed to get location|Invalid device id|Track starting point|Latest location|Start|Current|Fleet|Track|Location access needed|Live sync ready" app/src/main
```

Expected: no user-visible English matches in the touched UI paths

- [ ] **Step 4: Review the final diff for accidental behavior changes**

Run:

```bash
git diff -- app/src/main/res/values/strings.xml app/src/main/res/values/plurals.xml app/src/main/java/com/dzf/app/ui/MainActivity.kt app/src/main/java/com/dzf/app/ui/DeviceTrackActivity.kt gradle/wrapper/gradle-wrapper.jar
```

Expected: only localization text changes, string-resource substitutions, and the repaired wrapper JAR replacement
