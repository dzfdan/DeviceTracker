import pytest

from tests.appium.flows.permission_flow import PermissionFlow


@pytest.mark.smoke
@pytest.mark.local_device
def test_app_launch_and_permissions(pages):
    PermissionFlow(pages).accept_runtime_permissions()
    pages["main"].wait_loaded()
