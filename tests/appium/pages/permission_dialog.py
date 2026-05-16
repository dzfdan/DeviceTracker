from tests.appium.pages.base_page import BasePage


class PermissionDialog(BasePage):
    ALLOW_TEXTS = [
        "允许",
        "仅在使用应用时允许",
        "While using the app",
        "Allow",
    ]

    def allow_if_present(self):
        from appium.webdriver.common.appiumby import AppiumBy

        for text in self.ALLOW_TEXTS:
            elements = self.driver.find_elements(AppiumBy.XPATH, f"//*[@text='{text}']")
            if elements:
                elements[0].click()
                return True
        return False
