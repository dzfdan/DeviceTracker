# Appium Local Device Guide

## Recommended Runtime
- Recommended: run Appium and pytest on Windows
- Optional: maintain the test suite in WSL, but use Windows for real-device execution

## Prerequisites
- Python 3.11+
- Appium 2
- adb
- Android real device with USB debugging enabled

## Windows Workflow

### Install
```powershell
python -m pip install -r requirements-appium.txt
npm install -g appium
appium driver install uiautomator2@4.2.9
```

说明：`uiautomator2@4.2.9` 与 `Appium 2` 兼容，不能直接使用最新版本。

### Config
```powershell
copy tests\appium\config\device.example.yaml tests\appium\config\device.yaml
copy tests\appium\config\env.example.yaml tests\appium\config\env.yaml
```

Update `tests/appium/config/device.yaml` with your real `adb` device ID.

### Check device
```powershell
adb devices -l
```

### Start Appium Server
```powershell
appium
```

### Run smoke
```powershell
python -m pytest tests/appium/smoke -m "smoke and local_device" -v
```

### Run business flows
```powershell
python -m pytest tests/appium/business -m "business and local_device" -v
```

### Run HTML report
```powershell
python -m pytest tests/appium/smoke -m "smoke and local_device" -v --html=tests/appium/artifacts/smoke-report.html
```

## WSL Workflow

### Install
```bash
python3 -m pip install --user --break-system-packages -r requirements-appium.txt
npm install -g appium
appium driver install uiautomator2@4.2.9
```

说明：`uiautomator2@4.2.9` 与 `Appium 2` 兼容，不能直接使用最新版本。

### Config
```bash
cp tests/appium/config/device.example.yaml tests/appium/config/device.yaml
cp tests/appium/config/env.example.yaml tests/appium/config/env.yaml
```

Update `tests/appium/config/device.yaml` with your real `adb` device ID.

If `adb` is not on your Linux `PATH` in WSL, point to the Windows SDK binary explicitly:

```bash
export ADB_PATH="/mnt/c/Users/neusoft/AppData/Local/Android/Sdk/platform-tools/adb.exe"
```

### Start Appium Server
```bash
appium
```

### Run smoke
```bash
python3 -m pytest tests/appium/smoke -m "smoke and local_device" -v
```

### Run business flows
```bash
python3 -m pytest tests/appium/business -m "business and local_device" -v
```

### Run HTML report
```bash
python3 -m pytest tests/appium/smoke -m "smoke and local_device" -v --html=tests/appium/artifacts/smoke-report.html
```

## Known WSL Limitation
- Windows `adb.exe` can see the USB device, but WSL Appium sessions still fail in this environment
- The main issue is that Appium running in WSL passes Linux paths to helper APKs, while Windows `adb.exe` cannot access those paths
- Native Linux `adb` inside WSL also cannot currently see the same USB-connected device on this machine
- Because of that, real-device execution should currently be done on Windows

## Troubleshooting
- `adb devices` sees no device: check USB debugging
- `adb` not found in WSL: set `ADB_PATH` to the Windows SDK `adb.exe`
- Appium cannot connect: confirm Appium server is running
- WSL session creation fails: switch to the Windows workflow above
- permission dialog text differs: extend `PermissionDialog.ALLOW_TEXTS`
- unstable elements: prefer resource-id or contentDescription
