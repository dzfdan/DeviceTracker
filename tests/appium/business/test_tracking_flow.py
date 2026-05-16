import pytest

from tests.appium.flows.tracking_flow import TrackingFlow


@pytest.mark.business
@pytest.mark.local_device
def test_start_and_stop_tracking(pages):
    flow = TrackingFlow(pages)
    pages["main"].wait_loaded()
    flow.toggle_tracking()
    flow.toggle_tracking()
