from tests.appium.utils.waits import wait_visible


class BasePage:
    def __init__(self, driver):
        self.driver = driver

    def find_visible_by_id(self, resource_id: str, timeout: int = 15):
        from appium.webdriver.common.appiumby import AppiumBy

        return wait_visible(self.driver, (AppiumBy.ID, resource_id), timeout)

    def find_visible_by_accessibility(self, text: str, timeout: int = 15):
        from appium.webdriver.common.appiumby import AppiumBy

        return wait_visible(self.driver, (AppiumBy.ACCESSIBILITY_ID, text), timeout)

    def tap_id(self, resource_id: str):
        self.find_visible_by_id(resource_id).click()
