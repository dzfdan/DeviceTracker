import importlib
import sys
from pathlib import Path

import pytest
import yaml

from tests.appium.utils.adb_helpers import get_stay_awake_setting
from tests.appium.utils.adb_helpers import set_stay_awake_setting
from tests.appium.utils.adb_helpers import wake_and_unlock_device


def load_yaml(path: str) -> dict:
    return yaml.safe_load(Path(path).read_text(encoding="utf-8"))


def import_appium_modules():
    original_path = sys.path[:]
    removed_modules = {}
    try:
        sys.path = [
            path for path in sys.path
            if path != "tests" and (not path or Path(path).name != "tests")
        ]
        for name in ["appium", "appium.webdriver", "appium.options", "appium.options.android"]:
            if name in sys.modules:
                removed_modules[name] = sys.modules.pop(name)
        importlib.invalidate_caches()
        webdriver = importlib.import_module("appium.webdriver")
        options = importlib.import_module("appium.options.android").UiAutomator2Options
        return webdriver, options
    finally:
        sys.path = original_path
        for name, module in removed_modules.items():
            if name not in sys.modules:
                sys.modules[name] = module


@pytest.fixture(scope="session")
def appium_config():
    return load_yaml("tests/appium/config/env.yaml")


@pytest.fixture(scope="session")
def device_config():
    return load_yaml("tests/appium/config/device.yaml")


@pytest.fixture(scope="session")
def driver(appium_config, device_config):
    webdriver, UiAutomator2Options = import_appium_modules()
    stay_awake_setting = get_stay_awake_setting(device_config["udid"])

    try:
        set_stay_awake_setting(device_config["udid"], "3")
        wake_and_unlock_device(device_config["udid"])

        options = UiAutomator2Options().load_capabilities(device_config)
        drv = webdriver.Remote(appium_config["appiumServerUrl"], options=options)
        yield drv
        drv.quit()
    finally:
        set_stay_awake_setting(device_config["udid"], stay_awake_setting or "0")
