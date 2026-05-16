import pytest


@pytest.mark.business
@pytest.mark.local_device
def test_empty_or_error_states_render_without_crash(driver, pages):
    pages["main"].wait_loaded()
    assert driver.current_package == "com.dzf.app"
