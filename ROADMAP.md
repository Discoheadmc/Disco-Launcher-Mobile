# ROADMAP — Disco Launcher Mobile

Açık konuşmalı yol haritası. Her aşamanın neyi doğruladığı ve neyin
eksik olduğu aşağıda.

## ✅ Aşama 0 — İskelet ve yönetim (0.1.0, mevcut)

- [x] Kotlin + Jetpack Compose uygulaması, Prism-minimal koyu tema,
      8 vurgu rengi, özel Başlat butonu rengi
- [x] Çevrimdışı hesaplar (vanilla uyumlu offline UUID)
- [x] Microsoft hesapları: device code → XBL → XSTS → Minecraft profile
- [x] Vanilla kurulum: client jar, kütüphaneler (rule filtreli), asset index + nesneler
- [x] Instance yönetimi, sunucu adresi/RAM ayarları
- [x] Modrinth mağaza: arama + .mrpack kurulumu (downloads + overrides)
- [x] Runtime yönetici: .zip/.tar.xz JRE paketleri (Pojav openjdk kataloğu)
- [x] JVM komut zinciri (classpath/-Xmx/assets/account) + process başlatma
- [x] GitHub Actions CI (debug APK artifact)

**Doğrulama:** launcher'daki her şey gerçek; APK kurulup sürüm indirilebilir,
instance hazırlanabilir, JRE çıkarılabilir, JVM süreci boot edilir.

## 🚧 Aşama 1 — Oyunu gerçekten çalıştırmak (0.2.x)

Hedef: 1.16+ vanilla'nın Android'de açılıp ana menüyü göstermesi.

- [ ] **LWJGL-Android köprüsü**: PojavLauncher'ın yaptığı gibi `org.lwjgl`'ın
      Android uyarlamasını runtime'a enjekte et (GLFW → Android GLBC/Vulkan katmanı)
- [ ] **Natives seçimi**: kütüphane indirmede classifier'lı native artifact'ları
      (linux/arm64) indir ve `java.library.path`'e bağla
- [ ] **JNI/custom-launch alternatifi**: process başlatma `targetSdk 28`'e
      bağlı; JNI tabanlı launch ile modern hedef SDK'ya geçiş
- [ ] Dokunmatik kontrol düzeni + sanal fare/klavye (ZalithLauncher2 referans)
- [ ] OpenGL sürüm uyarıları ve cihaz uyumluluk matrisi

**Kaba süre:** deneyimli bir geliştirici için haftalar; mevcut iskeletle
(Pojav'ın açık kaynak bileşenlerini entegre ederek) daha hızlı.

## 🚧 Aşama 2 — Masaüstü ile parite (0.3.x)

- [ ] Fabric/Forge/NeoForge kurulumu (inheritsFrom zinciri)
- [ ] CurseForge desteği (kullanıcının kendi API anahtarı, masaüstündeki gibi)
- [ ] Kaplama & pelerin yönetimi (yerel yggdrasil ucu ile çevrimdışı skin)
- [ ] instance dışa/içe aktarma (masaüstü ile klasör uyumu)
- [ ] split-screen/tablet düzenleri, 8+ dil desteği (i18n)

## 🚧 Aşama 3 — Dağıtım (0.4.x)

- [ ] Release APK (imzalı) + sürüm sayfası
- [ ] F-Droid / Play Store değerlendirmesi (targetSdk yükseltme buraya bağlı)
- [ ] Otomatik güncelleme yok — masaüstü felsefesiyle aynı

## Bilinen sınırlar (0.1.0)

- Oyun GL init'inde durur: LWJGL-Android köprüsü olmadan JVM ana menüye
  ulaşamaz (Aşama 1). Kurulum/çalıştırma günlüğünde net hata görünür.
- **1.20.5+ Java 21 gerektirir**; katalogda henüz JRE 21 yok (Pojav
  depolarında sadece jre17/jre8 var). JRE 17 ile 1.17–1.20.4 çalışır.
- `targetSdk` bilinçli olarak 28: exec() izni için. Aşama 1'de JNI ile yükselir.
- JRE, exec izni olan **dahili depoda** tutulur (harici depolama `noexec`).
- SHA1 doğrulaması atlanıyor (indirmeler boyut kontrolüyle).
- Mod yükleyiciler ve natives classifier'ları henüz yok.
- Microsoft client_id varsayılan boş — Ayarlar'dan girilmeli (kendi Azure
  uygulaman ücretsizdir).
