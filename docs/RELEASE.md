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

## ساختن نسخه

```bash
git tag v0.1.0
```

```bash
git push origin v0.1.0
```

اگر Secrets نباشد، کار با خطای روشن متوقف می‌شود و هیچ فایلی منتشر نمی‌شود. متن تغییرات فارسی هر نسخه در `docs/releases/<tag>.md` است.
