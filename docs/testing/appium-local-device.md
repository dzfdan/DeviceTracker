# Appium Local Device Guide

## Prerequisites
- Python 3.11+
- Appium 2
- adb
- Android real device with USB debugging enabled

## Install
```bash
python3 -m pip install --user --break-system-packages -r requirements-appium.txt
npm install -g appium
appium driver install uiautomator2@4.2.9
```

说明：`uiautomator2@4.2.9` 与 `Appium 2` 兼容，不能直接使用最新版本。

## Config
```bash
cp tests/appium/config/device.example.yaml tests/appium/config/device.yaml
cp tests/appium/config/env.example.yaml tests/appium/config/env.yaml
```

Update `tests/appium/config/device.yaml` with your real `adb` device ID.

If `adb` is not on your Linux `PATH` in WSL, point to the Windows SDK binary explicitly:

```bash
export ADB_PATH="/mnt/c/Users/neusoft/AppData/Local/Android/Sdk/platform-tools/adb.exe"
```

## Run smoke
```bash
python3 -m pytest tests/appium/smoke -m "smoke and local_device" -v
```

启动 Appium Server：

```bash
appium
```

## Run business flows
```bash
python3 -m pytest tests/appium/business -m "business and local_device" -v
```

## Run HTML report
```bash
python3 -m pytest tests/appium/smoke -m "smoke and local_device" -v --html=tests/appium/artifacts/smoke-report.html
```

## Troubleshooting
- `adb devices` sees no device: check USB debugging
- `adb` not found in WSL: set `ADB_PATH` to the Windows SDK `adb.exe`
- Appium cannot connect: confirm Appium server is running
- permission dialog text differs: extend `PermissionDialog.ALLOW_TEXTS`
- unstable elements: prefer resource-id or contentDescription
