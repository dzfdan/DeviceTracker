import os
import subprocess


def adb_command() -> str:
    return os.environ.get("ADB_PATH", "adb")


def list_devices() -> list[str]:
    result = subprocess.run([adb_command(), "devices"], capture_output=True, text=True, check=True)
    return [
        line.split()[0]
        for line in result.stdout.splitlines()[1:]
        if line.strip() and "\tdevice" in line
    ]


def wake_and_unlock_device(device_id: str) -> None:
    subprocess.run([adb_command(), "-s", device_id, "shell", "input", "keyevent", "KEYCODE_WAKEUP"], check=True)
    subprocess.run([adb_command(), "-s", device_id, "shell", "wm", "dismiss-keyguard"], check=True)
    subprocess.run([adb_command(), "-s", device_id, "shell", "input", "swipe", "500", "1800", "500", "400"], check=True)


def get_stay_awake_setting(device_id: str) -> str:
    result = subprocess.run(
        [adb_command(), "-s", device_id, "shell", "settings", "get", "global", "stay_on_while_plugged_in"],
        capture_output=True,
        text=True,
        check=True,
    )
    return result.stdout.strip()


def set_stay_awake_setting(device_id: str, value: str) -> None:
    subprocess.run(
        [adb_command(), "-s", device_id, "shell", "settings", "put", "global", "stay_on_while_plugged_in", value],
        check=True,
    )


def start_activity(device_id: str, activity: str) -> None:
    subprocess.run(
        [
            adb_command(),
            "-s",
            device_id,
            "shell",
            "am",
            "start",
            "-W",
            "-n",
            activity,
        ],
        check=True,
    )
