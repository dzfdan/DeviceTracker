from tests.appium.pages.base_page import BasePage
from tests.appium.utils.locators import MAIN


class MainPage(BasePage):
    def wait_loaded(self):
        self.find_visible_by_id(MAIN["hud_strip"])
        self.find_visible_by_id(MAIN["track_fab"])
        self.find_visible_by_id(MAIN["my_location_fab"])

    def start_or_stop_tracking(self):
        self.tap_id(MAIN["track_fab"])

    def open_device_list(self):
        self.tap_id(MAIN["device_count_card"])

    def open_tracking_panel(self):
        self.tap_id(MAIN["track_card"])
