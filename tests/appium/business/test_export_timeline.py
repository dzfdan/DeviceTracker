import pytest


@pytest.mark.business
@pytest.mark.local_device
def test_export_entry_accessible(driver, pages):
    pages["main"].wait_loaded()
    assert driver.current_package == "com.dzf.app"
