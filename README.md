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
