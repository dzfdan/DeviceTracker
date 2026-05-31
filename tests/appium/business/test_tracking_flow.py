import pytest

from tests.appium.flows.navigation_flow import NavigationFlow


@pytest.mark.business
@pytest.mark.local_device
def test_home_fleet_card_opens_device_list(pages):
    pages["main"].wait_loaded()
    NavigationFlow(pages).open_device_list()
