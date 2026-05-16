# Device Tracker - Android App

基于高德地图和腾讯云开发的 Android 设备定位应用，实时显示所有安装了此应用的设备在地图上的位置。

## 功能特性

- 高德地图实时显示所有在线设备
- 高德定位 SDK 获取设备位置
- 腾讯云开发 CloudBase 实现设备位置同步（无需搭建后端服务器）
- 自定义 Marker 区分当前设备、在线设备和离线设备
- 点击 Marker 显示设备信息（设备名称、在线状态、最后更新时间）
- 后台位置追踪服务
- 自动设备识别（唯一设备 ID）

## 项目结构

```
DeviceTracker/
├── app/
│   ├── src/main/
│   │   ├── java/com/devicetracker/
│   │   │   ├── DeviceTrackerApp.kt
│   │   │   ├── model/
│   │   │   │   └── DeviceLocation.kt
│   │   │   ├── service/
│   │   │   │   ├── CloudBaseRepository.kt      # 腾讯云开发数据库通信
│   │   │   │   └── LocationTrackingService.kt  # 高德定位后台服务
│   │   │   ├── ui/
│   │   │   │   └── MainActivity.kt             # 高德地图主界面
│   │   │   └── util/
│   │   │       ├── AMapLocationManager.kt      # 高德定位管理
│   │   │       ├── DeviceInfoHelper.kt
│   │   │       └── PermissionHelper.kt
│   │   ├── res/layout/
│   │   │   ├── activity_main.xml               # MapView 布局
│   │   │   └── info_window.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

## 前置要求

1. **Android Studio** (推荐最新稳定版)
2. **高德开放平台账号** - 从 [高德开放平台](https://lbs.amap.com/) 获取 Key
3. **腾讯云账号** - 从 [腾讯云](https://cloud.tencent.com/) 开通

## 配置步骤

### 1. 获取高德地图 Key

1. 注册/登录 [高德开放平台](https://console.amap.com/)
2. 进入"应用管理" -> "我的应用" -> 创建新应用
3. 添加 Key，选择服务类型为 "Android平台"
4. 填写安全码（SHA1 + 包名 `com.devicetracker`）
5. 获取 Key 后在 `local.properties` 中配置：

```properties
AMAP_KEY=YOUR_AMAP_KEY_HERE
```

### 2. 开通腾讯云开发 CloudBase

#### 2.1 创建云开发环境

1. 登录 [腾讯云开发控制台](https://console.cloud.tencent.com/tcb)
2. 点击 **新建环境**
3. 选择 **按量计费**（新用户有免费额度）
4. 创建完成后记录 **环境 ID**（格式：`env-xxxxxx`）

#### 2.2 创建数据库集合

1. 进入环境 → **数据库** → **添加集合**
2. 集合名填写 `devices`
3. 无需预定义字段（文档型数据库，自动适配）

#### 2.3 配置数据库权限

1. 进入 `devices` 集合 → **权限设置**
2. 选择 **所有用户可读，仅创建者可读写**

#### 2.4 开启匿名登录

1. 进入环境 → **登录认证** → **匿名登录**
2. 点击 **启用**

### 3. 配置 Android 项目

1. 在 `CloudBaseRepository.kt` 中更新环境 ID（第 24 行）：

```kotlin
private const val ENV_ID = "your-env-id"
```

2. 复制 `local.properties.template` 为 `local.properties`，配置 SDK 路径和高德 Key

### 4. 构建和运行

```bash
./gradlew assembleDebug
```

或在 Android Studio 中点击 "Run" 按钮。

## 使用说明

1. 在多台 Android 设备上安装此应用
2. 授予位置权限
3. 应用自动开始追踪位置并上传到腾讯云开发
4. 高德地图上显示所有安装了此应用的设备
5. 点击 Marker 查看设备详情

## Appium 自动化测试

项目已增加基于 `Python + pytest + Appium 2 + UiAutomator2` 的本地真机自动化测试骨架，测试目录位于 `tests/appium/`。

### 运行方式选择

- 推荐：在 Windows 中运行 Appium 与 pytest
- 补充：可在 WSL 中维护测试代码和配置
- 当前结论：在这台机器上，Windows 真机连接最稳定，WSL 运行 Appium 仍存在设备桥接限制

### 前置条件

1. Python 3.11+
2. Node.js 与 npm
3. Appium 2
4. Android 真机，并开启 USB 调试
5. `adb` 可用

### 1. 安装 Python 依赖

```bash
python3 -m pip install --user --break-system-packages -r requirements-appium.txt
```

### 2. 安装 Appium 2 和 UiAutomator2 Driver

```bash
npm install -g appium
appium driver install uiautomator2@4.2.9
```

说明：`uiautomator2@4.2.9` 与当前方案中的 `Appium 2` 兼容，不能直接使用最新版本。

### 3. 准备测试配置

复制模板并生成本地配置文件：

```bash
cp tests/appium/config/device.example.yaml tests/appium/config/device.yaml
cp tests/appium/config/env.example.yaml tests/appium/config/env.yaml
```

修改 `tests/appium/config/device.yaml`：

- `udid`: 替换为 `adb devices` 查到的真实设备 ID
- `appPackage`: 默认是 `com.dzf.app`
- `appActivity`: 默认是 `com.dzf.app.ui.MainActivity`

`tests/appium/config/env.yaml` 默认内容：

```yaml
appiumServerUrl: http://127.0.0.1:4723
artifactsDir: tests/appium/artifacts
apkPath: app/build/outputs/apk/debug/app-debug.apk
```

### 4. 检查设备连接

```bash
adb devices
```

### 5. 推荐方案：在 Windows 中运行 Appium

先安装 Appium 和 Python 依赖：

```powershell
python -m pip install -r requirements-appium.txt
npm install -g appium
appium driver install uiautomator2@4.2.9
```

确认设备已连接：

```powershell
adb devices
```

启动 Appium Server：

```powershell
appium
```

运行测试：

```powershell
python -m pytest --collect-only tests/appium
python -m pytest tests/appium/smoke -m "smoke and local_device" -v
python -m pytest tests/appium/business -m "business and local_device" -v
python -m pytest tests/appium/smoke -m "smoke and local_device" -v --html=tests/appium/artifacts/smoke-report.html
```

这是当前最推荐的运行方式，因为 Windows 可以直接通过本机 `adb` 与 USB 真机通信。

### 6. 补充方案：在 WSL 中运行 Appium

WSL 适合维护测试代码、执行 `pytest --collect-only`、以及做纯 Python 层验证。

安装依赖：

```bash
python3 -m pip install --user --break-system-packages -r requirements-appium.txt
npm install -g appium
appium driver install uiautomator2@4.2.9
```

如果你只是想在 WSL 中复用 Windows 的 `adb.exe` 查看设备，可以设置：

```bash
export ADB_PATH="/mnt/c/Users/neusoft/AppData/Local/Android/Sdk/platform-tools/adb.exe"
```

可执行的基础检查：

```bash
python3 -m pytest --collect-only tests/appium
"/mnt/c/Users/neusoft/AppData/Local/Android/Sdk/platform-tools/adb.exe" devices -l
```

### 7. 已知限制

- 当前环境下，Windows `adb.exe` 可以看到 USB 真机，但 WSL 内原生 Linux `adb` 看不到该设备
- Appium 在 WSL 中运行时，如果依赖 Windows `adb.exe`，会在安装辅助 APK 时遇到 Linux 路径与 Windows 可访问路径不兼容的问题
- 因此，WSL 目前不适合作为这台机器上的 Appium 真机执行环境
- 推荐策略是：
  - 在 WSL 中编写和维护 `tests/appium/`
  - 在 Windows 中启动 Appium 并执行 pytest

### 8. 运行测试

先检查测试是否能被正确收集：

```bash
python3 -m pytest --collect-only tests/appium
```

运行 smoke 用例：

```bash
python3 -m pytest tests/appium/smoke -m "smoke and local_device" -v
```

运行 business 用例：

```bash
python3 -m pytest tests/appium/business -m "business and local_device" -v
```

生成 smoke HTML 报告：

```bash
python3 -m pytest tests/appium/smoke -m "smoke and local_device" -v --html=tests/appium/artifacts/smoke-report.html
```

在 Windows 中运行时，可将上面的 `python3` 替换为 `python`。

### 9. 常见问题

- `adb devices` 看不到设备：检查 USB 调试、数据线、设备授权弹窗
- `adb` 命令不存在：在 WSL 中设置 `ADB_PATH`
- Appium 启动了但测试连不上：确认 `tests/appium/config/env.yaml` 中的 `appiumServerUrl` 正确
- WSL 中真机会话起不来：优先改为 Windows 中执行 Appium 与 pytest
- 首次运行权限弹窗文本不一致：扩展 `tests/appium/pages/permission_dialog.py` 中的 `ALLOW_TEXTS`

## Marker 颜色说明

- **绿色**: 当前设备（你正在使用的设备）
- **蓝色**: 在线设备（60秒内有位置更新）
- **灰色**: 离线设备（超过60秒无位置更新）

## 技术栈

- **Kotlin** - Android 开发语言
- **高德地图 3D SDK** - 地图显示
- **高德定位 SDK** - 位置获取
- **腾讯云开发 CloudBase** - BaaS 后端服务（数据库 + 身份认证）
- **OkHttp + Gson** - HTTP 通信和 JSON 序列化
- **AndroidX + Material Design 3** - UI 组件

## 权限说明

| 权限 | 用途 |
|------|------|
| `ACCESS_FINE_LOCATION` | 获取精确位置 |
| `ACCESS_COARSE_LOCATION` | 获取大致位置 |
| `ACCESS_BACKGROUND_LOCATION` | 后台位置更新 |
| `FOREGROUND_SERVICE` | 前台位置服务 |
| `INTERNET` | 网络通信 |
| `POST_NOTIFICATIONS` | 服务通知（Android 13+） |
| `ACCESS_WIFI_STATE` | 高德定位辅助 |
| `CHANGE_WIFI_STATE` | 高德定位辅助 |

## 注意事项

- 高德地图 Key 需要正确配置 SHA1 和包名，否则地图无法加载
- 后台位置追踪会消耗电量
- 腾讯云开发有免费额度，超出后按量计费
- 建议在生产环境配置更严格的数据库安全规则
