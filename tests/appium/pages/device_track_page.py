from tests.appium.pages.base_page import BasePage


class DeviceTrackPage(BasePage):
    def wait_loaded(self):
        self.find_visible_by_id("com.dzf.app:id/trackSummaryMetaText")
