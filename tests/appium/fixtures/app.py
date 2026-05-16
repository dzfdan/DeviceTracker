import pytest

from tests.appium.pages.device_list_page import DeviceListPage
from tests.appium.pages.device_track_page import DeviceTrackPage
from tests.appium.pages.main_page import MainPage
from tests.appium.pages.permission_dialog import PermissionDialog


@pytest.fixture
def pages(driver):
    return {
        "permission": PermissionDialog(driver),
        "main": MainPage(driver),
        "device_list": DeviceListPage(driver),
        "device_track": DeviceTrackPage(driver),
    }
