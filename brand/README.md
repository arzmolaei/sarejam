# بسته برند سرجمع

همه چیزی که برای اپ، سایت sarejam.ir و شبکه‌های اجتماعی لازم است.

| پوشه | محتوا | کجا به کار می‌رود |
| --- | --- | --- |
| `logo/` | `sarejam-icon.svg` (آیکون اصلی)، `sarejam-icon-dark.svg`، `sarejam-mark.svg` (فقط نشان، بدون کادر)، نسخه سفید و مشکی | هر جا؛ SVG در هر اندازه تیز است |
| `lockup/` | لوگو + نام + شعار، افقی و عمودی، روشن و تیره (PNG شفاف) | سربرگ سایت، اسلاید، پوستر |
| `app-icon/` | `icon-1024.png`، `play-store-512.png`، لایه‌های adaptive و آیکون تک‌رنگ اندروید | اپ، کافه‌بازار، مایکت |
| `favicon/` | `favicon.ico`، `favicon.svg`، ۱۶/۳۲/۴۸، `apple-touch-icon.png`، `android-chrome-192/512`، `maskable-512` | `<head>` سایت |
| `social/` | `og-image.png` (۱۲۰۰×۶۳۰)، `avatar-400.png` | پیش‌نمایش لینک، پروفایل |
| `web/` | `hero-phone.png` (تصویر اصلی صفحه اول)، `icons/` (۸ آیکون ویژگی‌ها، مجوز ISC) | صفحه اول سایت |
| `colors/` | `tokens.css`، `tokens.json`، `palette.png` | سایت و اپ |
| `fonts/` | وزیرمتن (woff2، ۴ وزن، فارسی + لاتین) + `vazirmatn.css` + مجوز OFL | سایت؛ فونت را روی هاست خودت بگذار |

## قانون‌های استفاده

- لوگو را کش نده، نچرخان و رنگش را خارج از این بسته عوض نکن.
- دور لوگو حداقل به اندازه قطر سکه زعفرانی فضای خالی بگذار.
- روی پس‌زمینه شلوغ فقط آیکون با کادر (`sarejam-icon.svg`) استفاده کن.
- شعار: «پولت کجا رفت، کارت کجا رسید».

## نمونه `<head>` سایت

```html
<link rel="icon" href="/favicon.ico" sizes="any">
<link rel="icon" href="/favicon.svg" type="image/svg+xml">
<link rel="apple-touch-icon" href="/apple-touch-icon.png">
<meta property="og:image" content="https://sarejam.ir/og-image.png">
<meta name="theme-color" content="#0B7A6E">
<link rel="stylesheet" href="/fonts/vazirmatn.css">
<link rel="stylesheet" href="/colors/tokens.css">
```
