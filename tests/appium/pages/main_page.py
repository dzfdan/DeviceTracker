from tests.appium.pages.base_page import BasePage
from tests.appium.utils.locators import MAIN


class MainPage(BasePage):
    def wait_loaded(self):
        self.find_visible_by_id(MAIN["device_count_card"])
        self.find_visible_by_id(MAIN["my_location_fab"])

    def ensure_my_location_fab_visible(self):
        self.find_visible_by_id(MAIN["my_location_fab"])

    def open_device_list(self):
        self.tap_id(MAIN["device_count_card"])
