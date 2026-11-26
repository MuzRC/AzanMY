# 🕌 AzanMY

Aplikasi waktu solat rasmi Malaysia (**e-Solat JAKIM**) dan penggera azan tepat untuk Android — direka khusus dengan antaramuka kaca-metalik bertema **Midnight Emerald & Warm Gold**, sokongan penuh **Android 16 (API 36)**, serta ketahanan penggera latar belakang tanpa kompromi.

---

## ✨ Ciri-Ciri Utama

* **Jadual Rasmi e-Solat JAKIM:** Menyokong semua zon waktu solat seluruh Malaysia (Semenanjung, Sabah, Sarawak & Wilayah Persekutuan) lengkap dengan paparan tarikh Masihi, tarikh Hijrah, dan waktu Syuruk.
* **Kesan Zon Automatik (GPS) & Carian Manual:** Kesan zon JAKIM terdekat menggunakan lokasi sekali imbas tanpa membebankan bateri, atau cari secara manual mengikut kod zon, daerah, dan negeri.
* **Mod Luar Talian (*Offline-First*) & Auto-Segerak:** Jadual solat bulanan disimpan terus dalam peranti. Dilengkapi penyegerakan automatik setiap 7 hari serta pra-muat turun jadual bulan hadapan bermula setiap 25 haribulan.
* **Enjin Penggera Tepat & Kalis *Deep Doze*:** Menggunakan `AlarmManager` berketepatan tinggi dengan pemulihan automatik selepas peranti dimulakan semula (*reboot*), perubahan zon masa, atau kemas kini aplikasi.
* **Audio Azan `STREAM_ALARM` & Kawalan Pintar:**
  * Berbunyi melalui saluran penggera walaupun telefon dalam mod *Silent* atau *Do Not Disturb*.
  * **Auto-Naikkan Volume Penggera (80%):** Menaikkan paras bunyi penggera secara automatik ke 80% sekiranya terlalu perlahan atau senyap.
  * **Pengurusan `AudioFocus` & `MediaSession`:** Memperlahankan muzik atau Android Auto semasa azan berkumandang, berhenti automatik jika ada panggilan masuk, dan boleh dihentikan terus menggunakan butang *Volume* fizikal.
* **Pilihan Laungan Azan & Mod Per-Waktu:**
  * Azan Subuh khas (*As-Salatu Khairum Minan Naum*) dikunci secara automatik pada waktu Subuh.
  * 6 pilihan suara azan harian (JAKIM, Makkah, Madinah, Al-Aqsa, Syeikh Mishary, dan Takbir Raya) dengan butang uji pratonton terus.
  * Kawalan pantas **Azan / Getar / Senyap** bagi setiap waktu solat fardhu.
* **Kad Panel Notifikasi Utama & Widget `4x2`:** Paparan waktu solat seterusnya dan jadual harian terus di bahagian atas panel notifikasi (dengan butang pantas *Buka* & *Segerak*) serta widget skrin utama.
* **Kemas Kini Dalam Aplikasi (*In-App Updater*):** Semak versi baharu, muat turun APK, dan pasang kemas kini terus daripada *GitHub Releases*.

---

## 🛠️ Spesifikasi Teknikal

| Komponen | Butiran |
| :--- | :--- |
| **Bahasa & UI** | Kotlin, Jetpack Compose (Material 3) |
| **Keserasian SDK** | Min SDK 26 (Android 8.0) • Target & Compile SDK 36 (Android 16) |
| **Storan & Latar Belakang** | Jetpack DataStore Preferences, WorkManager, Foreground Service (`mediaPlayback`) |
| **Sumber Data** | Portal Rasmi e-Solat JAKIM (`e-solat.gov.my`) |
| **CI/CD** | GitHub Actions (Auto-Versioning, AI Release Notes, `zipalign` & `apksigner`) |

---

## 📥 Pemasangan

1. Buka halaman [**Releases**](../../releases) di repositori ini.
2. Muat turun fail **`AzanMY-vX.X.X-signed.apk`** yang terkini.
3. Pasang pada peranti Android anda dan lengkapkan 4 langkah persediaan awal (*Zon JAKIM*, *Notifikasi*, *Penggera Tepat*, dan *Abaikan Sekatan Bateri*) untuk prestasi penggera yang optimum.

---

## 👤 Pembangun

Dibangunkan oleh **Musa Bin Sarudin**  
*Beaufort, Sabah, Malaysia*
