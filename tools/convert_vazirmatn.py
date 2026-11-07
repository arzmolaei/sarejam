"""بازسازی فونت اندروید از فایل‌های وب موجود در مخزن.

پیش‌نیاز: python -m pip install 'fonttools[woff]'
اجرا از ریشهٔ مخزن: python tools/convert_vazirmatn.py
"""

from pathlib import Path
from tempfile import TemporaryDirectory

from fontTools.merge import Merger
from fontTools.ttLib import TTFont


WEIGHTS = {400: "regular", 500: "medium", 700: "bold", 800: "extra_bold"}


def main():
    root = Path(__file__).resolve().parents[1]
    work = root / "work"
    work.mkdir(exist_ok=True)
    destination = root / "core/ui/src/main/res/font"
    destination.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory(dir=work) as scratch:
        for weight, name in WEIGHTS.items():
            subsets = []
            source_timestamps = []
            for subset in ("arabic", "latin"):
                font = TTFont(root / f"brand/fonts/vazirmatn-{subset}-{weight}-normal.woff2")
                source_timestamps.append((font["head"].created, font["head"].modified))
                font.flavor = None
                font.recalcTimestamp = False
                converted = Path(scratch) / f"{subset}-{weight}.ttf"
                font.save(converted)
                font.close()
                subsets.append(str(converted))
            font = Merger().merge(subsets)
            # Keep the binary repeatable across machines and conversion dates.
            font.recalcTimestamp = False
            font["head"].created = min(item[0] for item in source_timestamps)
            font["head"].modified = max(item[1] for item in source_timestamps)
            font.save(destination / f"vazirmatn_{name}.ttf")
            font.close()


if __name__ == "__main__":
    main()
