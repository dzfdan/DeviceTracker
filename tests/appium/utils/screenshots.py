from datetime import datetime
from pathlib import Path


def save_failure_artifacts(driver, output_dir: str, test_name: str) -> None:
    target = Path(output_dir)
    target.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    screenshot = target / f"{test_name}-{stamp}.png"
    source = target / f"{test_name}-{stamp}.xml"
    try:
        driver.save_screenshot(str(screenshot))
    except Exception:
        pass

    try:
        source.write_text(driver.page_source, encoding="utf-8")
    except Exception:
        pass
