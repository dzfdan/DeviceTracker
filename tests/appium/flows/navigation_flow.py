class NavigationFlow:
    def __init__(self, pages):
        self.pages = pages

    def open_device_list(self):
        self.pages["main"].open_device_list()
        self.pages["device_list"].wait_loaded()

    def open_tracking_panel(self):
        self.pages["main"].open_tracking_panel()
        self.pages["device_track"].wait_loaded()
