import pytest

from tests.appium.utils.locators import MAIN


@pytest.mark.smoke
@pytest.mark.local_device
def test_home_hud_and_fabs_visible(driver, pages):
    pages["main"].wait_loaded()
    assert driver.find_element("id", MAIN["device_count_card"]).is_displayed()
    assert driver.find_element("id", MAIN["my_location_fab"]).is_displayed()
