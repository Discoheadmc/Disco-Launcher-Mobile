<div align="center">

<img src="docs/images/logo.png" alt="Disco Launcher Mobile" width="120"/>

# Disco Launcher Mobile

**Disco Launcher'ın Android portu — hızlı, hafif ve gizlilik dostu Minecraft başlatıcısı**

*Android port of [Disco Launcher](https://github.com/Discoheadmc/Disco-Launcher) (an XMCL-based desktop launcher)*

[![Build](https://github.com/Discoheadmc/Disco-Launcher-Mobile/actions/workflows/build.yml/badge.svg)](https://github.com/Discoheadmc/Disco-Launcher-Mobile/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/license-MIT-34D399.svg)](LICENSE)
[![Platform: Android](https://img.shields.io/badge/platform-Android-38BDF8.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%20%20%7C%20Compose-7C8CF8.svg)](https://kotlinlang.org)
[![Telemetri: yok](https://img.shields.io/badge/telemetri-none-A5B4FC.svg)](#)

[Özellikler](#-özellikler) · [Ekranlar](#-ekranlar) · [Kurulum](#-kurulum) · [Geliştirme](#-geliştirme) · [Yol Haritası](ROADMAP.md)

</div>

---

> ⚠️ **Durum (0.1.x):** Yönetim tarafı tam çalışır — hesaplar, sürüm/instance/mod paketi
> indirme ve arka plan indirmeleri dahil. Oyunun Android'de **grafik olarak açılması**
> (LWJGL-Android köprüsü) Aşama 1 işidir; bkz. [ROADMAP](ROADMAP.md).

## ✨ Özellikler

| | |
| --- | --- |
| 🎨 **Prism-minimal tema** | Masaüstündeki 8 vurgu rengi + özel "Başlat" butonu rengi, birebir palet (`#7C8CF8` / `#A5B4FC`) |
| 🔐 **Hesaplar** | Microsoft (WebView'siz cihaz kodu akışı: MSA → XBL → XSTS → Minecraft) + çevrimdışı (vanilla uyumlu UUID) |
| 🗂 **Çoklu instance** | Sürüm seçimi, RAM kaydırıcısı, sunucu adresi, per-instance ayarlar |
| ⬇️ **Zorunlu kurulum** | Instance oluşturunca sürüm + cihazına uygun Java runtime **otomatik** iner |
| 🌊 **Arka plan indirmeleri** | İndirme sırasında uygulamada gezinebilirsin; **ekran kapalıyken bile inmeye devam eder** (ön plan servisi + wake lock + bildirim); her indirmeyi tek tuşla **durdur**; yarım kalan dosyalar temizlenir, tamamlananlar korunur |
| 🛒 **Mağaza** | Modrinth mod paketleri — ikonlar, rozetler, popüler listesi, anahtar gerekmez |
| ☕ **Runtime yöneticisi** | PojavLauncherTeam openjdk JRE'leri (`.tar.xz`/`.zip`), cihaz mimarisi otomatik algılanır |
| 🚀 **JVM başlatma zinciri** | Classpath, `-Xmx`, ek JVM argümanları, asset/oyun argümanları; canlı oyun günlüğü |
| 🪶 **Gizlilik** | Telemetri yok, reklam yok; yalnızca Mojang / Modrinth / Microsoft uçlarına bağlanır |

## 📱 Ekranlar

| Ekran | Ne yapar |
| --- | --- |
| **Ana Ekran** | Instance ızgarası · arka plan indirme paneli (durdur ✓) · kurulum ilerlemeleri · Başlat |
| **Mağaza** | Modrinth keşfi — ikonlu kartlar, "MOD PACK" rozeti, indirme sayıları, popüler listesi |
| **Hesaplar** | Çevrimdışı ekleme · Microsoft cihaz kodu girişi · aktif hesap seçimi |
| **Sürümler** | Mojang manifest'i (release/snapshot) · tek dokunuşla arka plan kurulumu |
| **Runtime** | JRE kataloğu + özel URL kurulumu · indirme/durdur · kurulu listesi |
| **Ayarlar** | **Launcher:** varsayılan RAM, ek JVM argümanları, runtime seçimi · **Görünüm:** 8 renk + Başlat butonu · Microsoft client_id |

## 📦 Kurulum

1. **[Actions](https://github.com/Discoheadmc/Disco-Launcher-Mobile/actions)** → son yeşil `Build` koşusu → `DiscoLauncher-Mobile-debug` APK'sını indir
2. Eski kurulumu **kaldır** (debug imzaları build'ler arası farklıdır), yeni APK'yı kur
3. **Hesaplar** → hesap ekle (Microsoft için Ayarlar'a ücretsiz bir Azure `client_id` gir)
4. **Ana Ekran** → `+ Instance` → sürüm seç — kurulum ve JRE otomatik başlar
5. Kart → **Başlat**

> ℹ️ Oyun JVM'i başlatır ve günlüğü uygulamada gösterir. Grafik katmanı
> (LWJGL-Android) Aşama 1'de geliyor; o zamana kadar oyun ana menüye ulaşamaz —
> neden: [ROADMAP](ROADMAP.md#-aşama-1--oyunu-gerçekten-çalıştırmak-02x)

## 🛠 Geliştirme

**Stack:** Kotlin 2.0 · Jetpack Compose (BOM) · AGP 8.7 · Gradle 8.9 · minSdk 26

```bash
git clone https://github.com/Discoheadmc/Disco-Launcher-Mobile
cd Disco-Launcher-Mobile
./gradlew assembleDebug     # app/build/outputs/apk/debug/
```

- `targetSdk` bilinçli olarak 28 (JRE binary'sinin `exec` edilebilmesi için; JNI runtime ile yükseltilecek)
- Her push'ta CI debug APK üretir ve artifact olarak yayınlar

<details>
<summary><b>Mimari</b></summary>

```
app/src/main/java/com/discocock/launcher/
├── data/      # Hesaplar, instance'lar, ayarlar (DataStore + JSON)
├── di/        # AppContainer (manuel DI)
├── game/      # DownloadManager (arka plan + durdur), GameInstaller (vanilla),
│              # MrpackInstaller, RuntimeManager (JRE), JvmCommandBuilder,
│              # GameLauncher (process + log akışı)
├── net/       # MojangApi, ModrinthApi, MicrosoftAuth (device code → XBL → XSTS)
├── ui/        # Compose: Prism-minimal tema, navigasyon, 6 ekran
└── util/      # LauncherPaths, NetFiles (iptal edilebilir indirme), JsonFiles
```
</details>

## 🗺 Yol Haritası

| Aşama | İçerik | Durum |
| --- | --- | --- |
| **0** | Tema, hesaplar, sürüm/instance/mağaza, arka plan indirmeler, JVM zinciri | ✅ Tamamlandı |
| **1** | Oyunun grafik olarak açılması: LWJGL-Android köprüsü, natives, dokunmatik kontroller | 🚧 Sıradaki |
| **2** | Masaüstü paritesi: mod yükleyiciler, CurseForge, kaplama/pelerin | ⏳ Planlandı |
| **3** | Dağıtım: imzalı sürümler, F-Droid/Play değerlendirmesi | ⏳ Planlandı |

Ayrıntılar: [`ROADMAP.md`](ROADMAP.md)

## 🤝 Katkı & İletişim

Katkılar memnuniyetle! Issue aç, PR gönder — commit mesajları için
`feat:`, `fix:`, `chore:` kullan. Orijinal masaüstü launcher için
[XMCL ekibine](https://github.com/Voxelum/x-minecraft-launcher) teşekkürler.

## 📄 Lisans

[MIT](LICENSE) © 2026 Discoheadmc — masaüstü Disco Launcher ile aynı ruh:
açık, hafif, telemetrisiz.
