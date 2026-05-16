from datetime import datetime
from pathlib import Path


def save_failure_artifacts(driver, output_dir: str, test_name: str) -> None:
    target = Path(output_dir)
    target.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    screenshot = target / f"{test_name}-{stamp}.png"
    source = target / f"{test_name}-{stamp}.xml"
    driver.save_screenshot(str(screenshot))
    source.write_text(driver.page_source, encoding="utf-8")
