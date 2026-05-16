class PermissionFlow:
    def __init__(self, pages):
        self.pages = pages

    def accept_runtime_permissions(self, attempts: int = 2):
        for _ in range(attempts):
            if not self.pages["permission"].allow_if_present():
                break
