# انتشار نسخه (فایل نصبی امضاشده)

هر tag به شکل `v0.1.0` روی GitHub یک فایل نصبی امضاشده در صفحه Releases می‌سازد (`.github/workflows/release.yml`).
بررسی آپدیت داخل اپ هم بعداً از همین صفحه می‌خواند.

## کلید امضا

- کلید امضا (`*.jks`) **هرگز وارد مخزن نمی‌شود** و فقط در GitHub Secrets و یک نسخه آفلاین نزد صاحب پروژه است.
- گم شدن کلید یعنی هیچ کاربری دیگر نمی‌تواند آپدیت نصب کند؛ پس یک نسخه روی فلش و یک نسخه در جای امن دیگر نگه دار.

### Secrets لازم (GitHub → مخزن → Settings → Secrets and variables → Actions → New repository secret)

| نام | مقدار |
| --- | --- |
| `SAREJAM_KEYSTORE_BASE64` | محتوای فایل کلید به شکل base64 |
| `SAREJAM_KEYSTORE_PASSWORD` | رمز فایل کلید |
| `SAREJAM_KEY_ALIAS` | `sarejam` |
| `SAREJAM_KEY_PASSWORD` | رمز کلید (همان رمز فایل) |

### ساختن کلید روی کامپیوتر خودت (اگر JDK نصب است)

```bash
keytool -genkeypair -v -keystore sarejam-release.jks -alias sarejam -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Sarejam"
```

```bash
base64 -w0 sarejam-release.jks > sarejam-release.jks.b64
```

محتوای فایل `.b64` را در `SAREJAM_KEYSTORE_BASE64` بگذار.

## بدون Secrets (روش فعلی)

1. در GitHub → Actions → Release → Run workflow، نسخه را بنویس (مثلاً `0.1.0`).
2. فایل بدون امضا روی شاخه موقت `release-staging/v<نسخه>` قرار می‌گیرد (همراه `TAG` و `COMMIT`).
3. کسی که کلید را دارد امضا می‌کند: `apksigner sign --ks sarejam-release.jks --out signed.apk unsigned.apk`
   و `release/signed.apk` + `release/TAG` + `release/COMMIT` را در یک کامیت push می‌کند.
4. `publish-signed.yml` گواهی و نسخه را بررسی، tag و Release را روی همان کامیت ساخته‌شده ایجاد، و شاخه موقت را پاک می‌کند. آن کامیت فایل بعد از انتشار از شاخه برداشته می‌شود.

در هر دو روش، فایل فقط وقتی منتشر می‌شود که گواهی امضایش با اثر انگشت ثابت `docs/signing-cert.sha256` یکی باشد.

## ساختن نسخه

```bash
git tag v0.1.0
```

```bash
git push origin v0.1.0
```

اگر Secrets نباشد، کار با خطای روشن متوقف می‌شود و هیچ فایلی منتشر نمی‌شود. متن تغییرات فارسی هر نسخه در `docs/releases/<tag>.md` است.
