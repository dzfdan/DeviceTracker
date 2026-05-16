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
