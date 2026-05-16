from pathlib import Path

import pytest
import yaml


def load_yaml(path: str) -> dict:
    return yaml.safe_load(Path(path).read_text(encoding="utf-8"))


@pytest.fixture(scope="session")
def appium_config():
    return load_yaml("tests/appium/config/env.yaml")


@pytest.fixture(scope="session")
def device_config():
    return load_yaml("tests/appium/config/device.yaml")


@pytest.fixture(scope="session")
def driver(appium_config, device_config):
    from appium import webdriver
    from appium.options.android import UiAutomator2Options

    options = UiAutomator2Options().load_capabilities(device_config)
    drv = webdriver.Remote(appium_config["appiumServerUrl"], options=options)
    yield drv
    drv.quit()
