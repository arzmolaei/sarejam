"""بازسازی فونت اندروید از فایل‌های وب موجود در مخزن.

پیش‌نیاز: python -m pip install 'fonttools[woff]'
اجرا از ریشهٔ مخزن: python tools/convert_vazirmatn.py
"""

from pathlib import Path
from tempfile import TemporaryDirectory

from fontTools.merge import Merger
from fontTools.ttLib import TTFont


def main():
    root = Path(__file__).resolve().parents[1]
    work = root / "work"
    work.mkdir(exist_ok=True)
    destination = root / "core/ui/src/main/res/font/vazirmatn_regular.ttf"
    destination.parent.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory(dir=work) as scratch:
        subsets = []
        for subset in ("arabic", "latin"):
            font = TTFont(root / f"brand/fonts/vazirmatn-{subset}-400-normal.woff2")
            font.flavor = None
            converted = Path(scratch) / f"{subset}.ttf"
            font.save(converted)
            font.close()
            subsets.append(str(converted))
        font = Merger().merge(subsets)
        # Keep the binary repeatable across machines and conversion dates.
        font.recalcTimestamp = False
        font.save(destination)
        font.close()


if __name__ == "__main__":
    main()
