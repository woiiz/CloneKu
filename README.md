# CloneKu — App Cloner (by Paktam)

App Android untuk clone app lain dan install versi ke-2, ke-3 dalam telefon yang sama.
Function clone 100% sama macam app asal — sebab yang di-clone tu APK asal, bukan versi tiruan.

## Cara clone dia berfungsi (2 cara dalam 1 app)

### Cara 1 — Work Profile (UTAMA, paling stabil, tanpa root)
Android sendiri benarkan 1 Work Profile. App dalam Work Profile adalah installasi ke-2 yang berasingan data.
- Contoh: WhatsApp asal di Profile Utama, WhatsApp Clone di Work Profile — 2 nombor, 2 akaun, jalan serentak.
- Tak perlu ubah APK, tak perlu resign, Play Integrity / signature check tak pecah.
- Ini cara yang dipakai Shelter / Island.

Syarat: Telefon belum ada Work Profile. Kalau dah ada (telefon kerja), kena buang dulu.

### Cara 2 — Repack APK (fallback, tanpa Work Profile)
Copy APK asal → tukar package name jadi `com.asal.clone1` → sign semula → install macam app baru.
- Berfungsi untuk kebanyakan app biasa, game offline, app sosial biasa.
- TAK berfungsi / akan crash untuk: banking, WhatsApp (check signature), app yang guna Google Play Services dengan signature check, game online anti-cheat.

App ni akan cuba Cara 1 dulu, kalau gagal baru tawar Cara 2.

## Cara build APK (percuma, 10 minit)

### Di PC / laptop — Android Studio (paling senang)
1. Install Android Studio (percuma) dari developer.android.com/studio
2. Open project: pilih folder `app-cloner` ni
3. Tunggu Gradle sync siap
4. Build > Generate App Bundles / APKs > Generate APK(s)
5. APK keluar di `app/build/outputs/apk/debug/app-debug.apk`
6. Copy APK tu ke telefon, install, bagi permission yang diminta

### Di telefon Android saja (tanpa PC)
1. Install **AIDE** / **Sketchware** / **AndroidIDE** dari Play Store — ATAU
2. Paling senang: upload folder ni ke GitHub, guna **GitHub Actions** percuma untuk build APK (workflow dah disediakan dalam `.github/workflows/build.yml` — push je, APK auto-build dalam Actions > Artifacts)
3. Download APK dari Artifacts, install.

Minimum: Android 8.0 (API 26), target Android 14 (API 34). Tak perlu root.

## Cara guna
1. Buka CloneKu, tekan **"Aktifkan Work Profile"** — ikut arahan Android, set profile.
2. Pilih app nak clone dari senarai (contoh: TikTok, WhatsApp, Telegram, Facebook).
3. Tekan **Clone** — pilih nama clone, contoh: `WhatsApp 2`
4. Tunggu install siap — icon clone akan muncul dengan badge beg kerja (💼) di launcher, atau buka dari dalam CloneKu > tab **Clone Saya** > tekan **Buka**.
5. Nak clone lagi app sama kali ke-2? Work Profile cuma bagi 1 copy tambahan. Untuk copy ke-3, ke-4 guna Cara 2 Repack, atau tambah user profile lain.

## Kebenaran yang diminta
- `QUERY_ALL_PACKAGES` — senaraikan app yang dah install
- `REQUEST_INSTALL_PACKAGES` — install APK clone
- `READ/WRITE_EXTERNAL_STORAGE` (Android lama) — copy APK
- Device Admin / Profile Owner — untuk cipta Work Profile sahaja, bukan lock telefon

## Fail penting
- `app/src/main/java/com/paktam/cloneku/MainActivity.kt` — UI utama
- `app/src/main/java/com/paktam/cloneku/CloneManager.kt` — logik clone Work Profile + Repack
- `app/src/main/java/com/paktam/cloneku/ApkRepacker.kt` — copy APK, tukar package name, sign
- `app/src/main/AndroidManifest.xml` — permission + DeviceAdminReceiver

## Nota jujur
Aku tak boleh bagi APK siap terus dari sini sebab build APK perlukan Android SDK + signing — tapi semua source code dah lengkap, kau build sekali je, lepas tu APK tu kau punya selamanya, boleh share/install kat telefon lain.
Kalau kau nak aku buildkan APK siap, upload projek ni ke GitHub dan bagi aku link repo — aku boleh tolong setup GitHub Actions build.

## Nota FileProvider
Untuk install APK clone, tambah FileProvider dalam AndroidManifest.xml (provider `com.paktam.cloneku.provider` + `res/xml/file_paths.xml` dah disediakan). Android Studio akan warning kalau tertinggal — tambah sebelum build release.
