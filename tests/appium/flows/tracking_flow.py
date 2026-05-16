class TrackingFlow:
    def __init__(self, pages):
        self.pages = pages

    def toggle_tracking(self):
        self.pages["main"].start_or_stop_tracking()
