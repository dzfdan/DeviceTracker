from pathlib import Path

from tests.appium.fixtures.driver import import_appium_modules
from tests.appium.fixtures.driver import load_yaml
from tests.appium.utils.adb_helpers import list_devices
from tests.appium.utils.screenshots import save_failure_artifacts


class DummyDriver:
    page_source = "<hierarchy />"

    def __init__(self):
        self.screenshots = []

    def save_screenshot(self, path: str) -> None:
        self.screenshots.append(path)
        Path(path).write_bytes(b"png")


def test_list_devices_returns_only_connected_device_ids(monkeypatch):
    class Result:
        stdout = "List of devices attached\nemulator-5554\tdevice\nZX1G22\toffline\nR58M35\tdevice\n"

    def fake_run(command, capture_output, text, check):
        assert command == ["/custom/adb", "devices"]
        assert capture_output is True
        assert text is True
        assert check is True
        return Result()

    monkeypatch.setenv("ADB_PATH", "/custom/adb")
    monkeypatch.setattr("tests.appium.utils.adb_helpers.subprocess.run", fake_run)

    assert list_devices() == ["emulator-5554", "R58M35"]


def test_save_failure_artifacts_writes_screenshot_and_page_source(tmp_path):
    driver = DummyDriver()

    save_failure_artifacts(driver, str(tmp_path), "test_failure")

    png_files = list(tmp_path.glob("test_failure-*.png"))
    xml_files = list(tmp_path.glob("test_failure-*.xml"))

    assert len(png_files) == 1
    assert len(xml_files) == 1
    assert driver.screenshots == [str(png_files[0])]
    assert xml_files[0].read_text(encoding="utf-8") == "<hierarchy />"


def test_load_yaml_reads_yaml_mapping(tmp_path):
    config = tmp_path / "config.yaml"
    config.write_text("appiumServerUrl: http://127.0.0.1:4723\n", encoding="utf-8")

    assert load_yaml(str(config)) == {"appiumServerUrl": "http://127.0.0.1:4723"}


def test_import_appium_modules_uses_installed_package_when_tests_path_shadows_it(monkeypatch):
    import sys

    sys.path.insert(0, "tests")
    monkeypatch.syspath_prepend("tests")

    webdriver, options = import_appium_modules()

    assert webdriver.__name__ == "appium.webdriver"
    assert options.__name__ == "UiAutomator2Options"


def test_device_list_adapter_uses_survey_status_chip_tokens():
    adapter_source = Path(
        "app/src/main/java/com/dzf/app/ui/DeviceListAdapter.kt"
    ).read_text(encoding="utf-8")

    assert "R.drawable.bg_status_chip" in adapter_source
    assert "bg_chip_status_online" not in adapter_source
    assert "bg_chip_status_offline" not in adapter_source


def test_tool_button_drawable_defines_disabled_state():
    drawable_source = Path(
        "app/src/main/res/drawable/bg_button_tool.xml"
    ).read_text(encoding="utf-8")

    assert 'android:state_enabled="false"' in drawable_source


def test_marker_ui_style_does_not_use_const_resource_ids():
    activity_source = Path(
        "app/src/main/java/com/dzf/app/ui/MainActivity.kt"
    ).read_text(encoding="utf-8")

    assert "const val COLOR_CURRENT" not in activity_source
    assert "const val COLOR_ONLINE" not in activity_source
    assert "const val COLOR_OFFLINE" not in activity_source
