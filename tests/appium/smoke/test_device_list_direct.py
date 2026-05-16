import pytest

from tests.appium.utils.adb_helpers import start_activity


@pytest.mark.smoke
@pytest.mark.local_device
def test_device_list_direct_launch(driver, pages, device_config):
    start_activity(device_config["udid"], "com.dzf.app/com.dzf.app.ui.DeviceListActivity")
    pages["device_list"].wait_loaded()
    assert driver.current_package == "com.dzf.app"
