# Disco Launcher Mobile

<p align="center">
  <b>Disco Launcher'ın Android portu — Kotlin + Jetpack Compose ile yazılmış hızlı, hafif ve gizlilik dostu Minecraft başlatıcısı.</b><br/>
  Android port of <a href="https://github.com/Discoheadmc/Disco-Launcher">Disco Launcher</a> (an XMCL-based launcher).
</p>

> ⚠️ **Erken geliştirme (0.1.x)** — Arayüz, hesaplar, sürüm/instance/modpack yönetimi tam çalışır durumda.
> Oyunu Android'de gerçekten başlatma (runtime entegrasyonu) için [ROADMAP](ROADMAP.md)'e bakın.

## ✨ Özellikler

- 🎨 **Prism-minimal tema** — masaüstündeki 8 vurgu rengi seçici + özel "Başlat" butonu rengi (birebir uyumlu: `#7C8CF8` / `#A5B4FC`)
- 🔐 **Microsoft + çevrimdışı hesaplar** — Microsoft, WebView gerektirmeyen cihaz kodu (device code) akışıyla; çevrimdışı hesaplar vanilla uyumlu deterministik UUID alır
- 🗂 **Çoklu instance** — oluştur/sil, sürüm seç, sunucu adresi, RAM ayarı
- ⬇️ **Vanilla sürüm kurulumu** — piston-meta manifest'ten client jar + kütüphaneler + varlıklar (assets), kurulum sırasında canlı ilerleme
- 🛒 **Mağaza** — Modrinth mod paketi arama; `.mrpack` indirme ve instance'a kurma (index çözümleme + overrides)
- ☕ **Java Runtime yöneticisi** — PojavLauncherTeam openjdk derlemeleri (.tar.xz/.zip) indirme, ayıklama, listeleme
- 🚀 **JVM komut zinciri** — classpath, `-Xmx`, asset/oyun argümanları masaüstü kurallarıyla üretilir; çıkarılan JRE process olarak başlatılır
- 📜 **Kurulum & başlatma günlüğü** — canlı log akışı
- 🪶 **Gizlilik** — telemetri yok, reklam yok; yalnızca Mojang/Modrinth/Microsoft uçlarına bağlanır

## 📸 Ne içerir?

| Ekran | İşlev |
| --- | --- |
| Ana Ekran | Instance ızgarası, hızlı eylemler, kurulum ilerlemesi, başlatma |
| Mağaza | Modrinth mod paketi arama + kurulum |
| Hesaplar | Çevrimdışı ekleme, Microsoft cihaz kodu girişi, aktif hesap seçimi |
| Ayarlar | 8 renk seçici, Başlat butonu rengi, Microsoft client_id, hakkında |
| Sürümler | Manifest listesi (release/snapshot), tek dokunuşla kurulum |
| Runtime | JRE katalog + özel URL kurulumu |

## 🆚 Masaüstü Disco Launcher Farkı

| Alan | Disco Launcher (masaüstü) | Disco Launcher Mobile |
| --- | --- | --- |
| Teknoloji | Electron + Vue (XMCL fork) | Kotlin + Jetpack Compose (sıfırdan) |
| Hesaplar | Microsoft + çevrimdışı | Microsoft (device code) + çevrimdışı |
| Sürümler | Vanilla + mod yükleyicileri | Şimdilik vanilla (mod yükleyicileri yol haritasında) |
| Mağaza | CurseForge + Modrinth + FTB | Modrinth (anahtar gerektirmez) |
| Oyun başlatma | Tam sürüm | JVM ayağa kalkar; GL/LWJGL köprüsü Aşama 2'de |
| Kaplamalar/pelerin | Yerel yggdrasil | Yol haritasında |

## 📦 Kurulum (kullanıcı)

1. [Actions](https://github.com/Discoheadmc/Disco-Launcher-Mobile/actions) sekmesindeki son başarılı `Build` işinden `DiscoLauncher-Mobile-debug` APK'sını indir
2. APK'yı telefona kur ("bilinmeyen kaynaklar"a izin ver)
3. **Hesaplar** → çevrimdışı hesap ekle (Microsoft için Ayarlar'a client_id gir)
4. **Ana Ekran** → instance oluştur → sürümü kur
5. **Java Runtime** → cihazına uygun JRE'yi kur (çoğu telefonda `jre17-arm64`)
6. Instance kartına dokun → **Başlat**

## 🛠 Geliştirme

Gereksinimler: JDK 17, Android SDK (AGP 8.7.3 · Gradle 8.9 · Kotlin 2.0.21).

```bash
git clone https://github.com/Discoheadmc/Disco-Launcher-Mobile
cd Disco-Launcher-Mobile
./gradlew assembleDebug        # app/build/outputs/apk/debug/
```

CI: her push'ta GitHub Actions debug APK üretir ve artifact olarak yükler.

### Microsoft girişini etkinleştirmek (isteğe bağlı)

[azure.microsoft.com](https://portal.azure.com) üzerinden ücretsiz bir uygulama kaydı aç (herkese açık istemci + "Xbox Live" izni), `client_id`'yi uygulama içi Ayarlar'a yapıştır. `SettingsRepository.DEFAULT_MS_CLIENT_ID` sabitine yazarak derleme zamanında gömebilirsin.

## 🧱 Mimari

```
app/src/main/java/com/discocock/launcher/
├── data/      # Hesap, instance, ayar depoları (DataStore + JSON dosyaları)
├── di/        # Manuel DI (AppContainer)
├── game/      # GameInstaller (vanilla), MrpackInstaller, RuntimeManager,
│              # JvmCommandBuilder, GameLauncher (process)
├── net/       # MojangApi, ModrinthApi, MicrosoftAuth (device code→XBL→XSTS→MC)
├── ui/        # Compose: tema (Prism-minimal), navigasyon, 6 ekran
└── util/      # LauncherPaths, NetFiles (indirme/arşiv), JsonFiles
```

## 📄 Lisans

[MIT](LICENSE) — masaüstü Disco Launcher gibi. XMCL'ye ait katkılar için orijinal launcher'a bakın.
