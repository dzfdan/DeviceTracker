class NavigationFlow:
    def __init__(self, pages):
        self.pages = pages

    def open_device_list(self):
        self.pages["main"].open_device_list()
        self.pages["device_list"].wait_loaded()
