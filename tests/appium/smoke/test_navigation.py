import pytest

from tests.appium.flows.navigation_flow import NavigationFlow


@pytest.mark.smoke
@pytest.mark.local_device
def test_navigation_home_shell_loaded(driver, pages):
    pages["main"].wait_loaded()
    assert driver.current_package == "com.dzf.app"


@pytest.mark.smoke
@pytest.mark.local_device
def test_navigation_to_device_list(pages):
    pages["main"].wait_loaded()
    NavigationFlow(pages).open_device_list()
