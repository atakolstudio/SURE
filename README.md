# SURE — Android TV Uzaktan Kumanda (IR + WiFi)

SURE, Android cihazınızı bir TV/klima/set üstü kutu vb. cihazlar için kızılötesi (IR)
uzaktan kumandaya dönüştüren, Kotlin + Jetpack Compose ile yazılmış production-ready
bir uygulamadır.

## Teknoloji Yığını

| Katman | Teknoloji |
|---|---|
| UI | Jetpack Compose + Material3 |
| Mimari | MVVM + Clean Architecture (data / domain / ui) |
| DI | Hilt |
| Yerel veritabanı | Room |
| Navigasyon | Navigation Compose |
| Eşzamanlılık | Kotlin Coroutines + Flow |
| Build | Gradle Kotlin DSL (`.kts`) |
| Min SDK / Target SDK | 24 / 35 |

## Proje Yapısı

```
app/src/main/java/com/atakolstudio/sure/
├── SureApplication.kt          # @HiltAndroidApp giriş noktası
├── MainActivity.kt             # Compose host + tema
├── data/
│   ├── ir/                     # IR protokol motoru
│   │   ├── IrProtocol.kt       # NEC, Samsung, Sony SIRC, RC5, RC6, Panasonic
│   │   ├── IrCodeEncoder.kt    # protokol -> ConsumerIrManager darbe dizisi
│   │   ├── RemoteButton.kt     # Tüm tuş türleri
│   │   ├── BrandIrDatabase.kt  # 25 marka için kod tabloları
│   │   └── IrTransmitter.kt    # ConsumerIrManager sarmalayıcı
│   ├── local/                  # Room (entity, dao, database)
│   └── repository/             # DeviceRepository
├── domain/model/                # DeviceType, ConnectionType
├── di/                          # Hilt modülleri (Database, Repository, App)
└── ui/
    ├── theme/                   # Material3 renk/tipografi (dark mode destekli)
    ├── navigation/               # NavGraph + rota tanımları
    ├── components/               # RemoteIconButton, RemoteTextButton
    └── screens/
        ├── devices/              # Ana ekran ("Cihazlarım")
        ├── devicetype/           # Cihaz türü seçimi
        ├── connectiontype/       # IR / WiFi seçimi
        ├── brand/                # Marka arama + seçimi
        └── remote/               # Asıl uzaktan kumanda ekranı
```

## Çalıştırma

1. Bu klasörü Android Studio (Koala veya üzeri) ile açın; Gradle senkronu otomatik
   olarak `gradle-wrapper.properties` üzerinden Gradle 8.7 + AGP 8.5.2'yi indirir.
2. IR sinyali gönderebilmek için **fiziksel bir Android TV kumandası / kızılötesi
   vericisi olan bir telefon** kullanın (`ConsumerIrManager.hasIrEmitter()` false
   dönerse buton arayüzü çalışır ama sinyal gönderilmez — uygulama bunu kullanıcıya
   üstte bir uyarı olarak gösterir, çökme yaşanmaz).
3. `Run ▶` ile cihaza/emülatöre yükleyin.

> Not: Android emülatörlerinde `CONSUMER_IR_SERVICE` donanımı yoktur; IR gönderimini
> gerçek bir cihazda test edin.

## IR Kod Mimarisi Nasıl Çalışır?

Her marka, `BrandIrDatabase.kt` içinde bir `BrandIrCodeSet` olarak tanımlanır:

```kotlin
BrandIrCodeSet(
    brandKey = "samsung",
    displayNameEn = "Samsung",
    displayNameLocal = "삼성",
    protocol = IrProtocol.SAMSUNG,
    verified = true,
    address = 0x07,
    commands = mapOf(
        RemoteButton.POWER to 0x02,
        RemoteButton.VOLUME_UP to 0x07,
        // ...
    )
)
```

Kullanıcı bir tuşa bastığında:

1. `RemoteViewModel.sendCommand(button)` çağrılır.
2. `IrTransmitter`, `BrandIrDatabase.toIrCommand(brand, button)` ile marka + tuşu
   bir `IrCommand(protocol, address, command)` üçlüsüne çevirir.
3. `IrCodeEncoder.encode(command)`, protokole özgü zamanlama kurallarını uygulayarak
   gerçek mikrosaniye darbe dizisini (`IntArray`) üretir.
4. `ConsumerIrManager.transmit(frequency, pattern)` çağrılarak sinyal fiziksel
   olarak gönderilir.

Bu katmanlı tasarım sayesinde **yeni bir protokol eklemek** `IrCodeEncoder`'a tek bir
`when` dalı eklemek, **yeni bir marka eklemek** ise `BrandIrDatabase.brands` listesine
tek bir `BrandIrCodeSet` daha eklemekten ibarettir.

### Yeni Marka / Kod Genişletme Rehberi

Bazı markalarda (özellikle daha az bilinen OEM'ler) kod tablosu **jenerik NEC evrensel
kod seti** (`verified = false`) ile doldurulmuştur. Bu kodlar birçok ucuz/OEM TV'de
çalışır ama garanti edilmez. Kendi TV'nizin gerçek kodlarını eklemek için:

1. Play Store'dan ücretsiz bir "IR remote analyzer" / "IR receiver" uygulaması kurun
   (bazı Android TV kutularında donanım IR alıcı bulunur).
2. Orijinal kumandanızın gönderdiği adres (address) ve komut (command) baytlarını
   okuyun.
3. `BrandIrDatabase.kt` içinde ilgili markayı bulun (yoksa yeni bir `BrandIrCodeSet`
   ekleyin) ve okuduğunuz değerleri `commands` haritasına girin:

```kotlin
BrandIrCodeSet(
    brandKey = "benim_markam",
    displayNameEn = "MyBrand",
    displayNameLocal = "MyBrand",
    protocol = IrProtocol.NEC, // veya SAMSUNG / SONY_SIRC12 / RC5 / RC6 / PANASONIC
    verified = true,
    address = 0xNN,
    commands = mapOf(
        RemoteButton.POWER to 0xNN,
        // ...
    )
)
```

4. `verified = true` yaparak arayüzdeki "Doğrulanmamış" etiketini kaldırın.

## Markası Bilinmeyen Cihazlar İçin "Manuel Bul"

Marka listesinde cihazınızı bulamıyorsanız veya markanız var ama kodu çalışmıyorsa,
Marka Seçimi ekranındaki **"Markamı Bilmiyorum"** kartına dokunun. İki yöntem sunulur:

1. **Kod Tarama**: Uygulama, üç kademeli bir aday listesini sırayla dener:
   - **Bilinen Markalar** (~25): veritabanındaki isimli markalar, her zaman aktif.
   - **Kör Tarama** (isteğe bağlı anahtar): LIRC açık kaynak veritabanının tamamı
     taranarak (~2650 kumanda konfigürasyon dosyası) elde edilen **371 benzersiz,
     GERÇEK kumanda kodu** (`assets/lirc_blind_scan.json`, bkz. `LircBlindScanLoader.kt`).
     Bu kodlar genelde sadece güç değil, ses/kanal/D-pad gibi birçok tuşu da içerir.
   - **Aşırı Tarama** (isteğe bağlı, ikinci anahtar): LIRC'te de karşılığı çıkmayan
     gerçekten kataloglanmamış cihazlar için son çare — NEC/Sony protokollerinde
     sistematik bir adres × komut ızgarası (`BlindScanCandidates.kt`), yalnızca güç
     tuşu test edilir, çok daha düşük isabet ihtimaliyle.

   Otomatik Tarama düğmesiyle (▶) elle "sıradaki" tıklamak yerine ~1.2 saniyede bir
   kendiliğinden ilerleyebilir; cihaz tepki verdiği an DURDUR'a basıp onaylarsınız.
2. **Elle Kod Gir**: Bir IR alıcı/analiz uygulamasıyla orijinal kumandanızın protokolünü,
   adresini ve komut baytını okuduysanız, bunları doğrudan girip test edebilir ve
   çalışırsa cihaz olarak kaydedebilirsiniz. NEC protokolünde, bulunan adresle diğer
   tuşlar (ses, kanal, D-pad) için de otomatik bir şablon uygulanır; diğer protokollerde
   şimdilik yalnızca test edilen (genelde Güç) tuşu kaydedilir.

Bu şekilde kaydedilen "özel" cihazlar, `SavedDeviceEntity` içinde `brandKey = "custom"`
ile ve kendi protokol/adres/komut bilgileriyle saklanır (bkz. `data/ir/SavedDeviceIrMapper.kt`).

## IR Kod Kaynağı: LIRC Veritabanı

Marka kod tablosundaki çoğu **doğrulanmış (verified=true)** kod seti, [LIRC (Linux
Infrared Remote Control) projesinin açık kaynak "remotes" veritabanından](https://lirc.sourceforge.net/remotes/)
(ayna: [github.com/probonopd/lirc-remotes](https://github.com/probonopd/lirc-remotes))
türetilmiştir. Bu veritabanı, gerçek kumandaların bir kızılötesi alıcısıyla okunup
analiz edilmesiyle oluşturulmuş, on yıllardır kullanılan bir açık kaynak kaynağıdır.

Bu sayede şu markalar artık **gerçek, doğrulanmış** kodlarla geliyor: Samsung, LG,
Sony, Philips (elle doğrulanmış) + Toshiba, Sharp, Vestel, Insignia, JVC, Telefunken,
Grundig, AOC, Sanyo, Vizio, Thomson, Daewoo, Akai, RCA, Orion, Polaroid, Goldstar,
Beko (LIRC kaynaklı). Bu süreçte ayrıca **JVC protokolü** de motora eklendi (16 bit,
tümleç yok, `IrProtocol.JVC`).

Hâlâ jenerik kod kullanan markalar (Hisense, TCL, Changhong, Konka, Skyworth, Regal,
Arçelik, Panasonic) için LIRC veritabanında doğrudan bir karşılık bulunamadı veya
desteklenmeyen bir protokol (Thomson-özel, Emerson, Aiwa, RECS80 vb.) kullanıyordu.

## İzinler

| İzin | Amaç |
|---|---|
| `TRANSMIT_IR` | Kızılötesi sinyal gönderimi (tehlikeli izin değildir, kurulumda otomatik verilir) |
| `INTERNET` | Gelecekteki WiFi/akıllı cihaz protokolleri için |
| `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Ağ durumu kontrolü |

`uses-feature android:name="android.hardware.consumerir" android:required="false"`
sayesinde uygulama, IR donanımı olmayan cihazlara da (Play Store'dan) kurulabilir.

## WiFi / Akıllı Cihaz Desteği

Bu sürümde "Akıllı Cihaz (WiFi)" bağlantı türü seçilebilir ve cihaz kaydedilebilir,
ancak gerçek HTTP/WebSocket protokol entegrasyonu henüz uygulanmamıştır
(`RemoteViewModel.sendCommand` içinde ilgili dal bir bilgi mesajı gösterir). Bunu
eklemek için:

1. `data/wifi/` altında marka bazlı bir istemci (ör. Samsung Smart TV WebSocket API,
   LG webOS SSAP, Roku ECP REST API) oluşturun.
2. `RemoteViewModel` içindeki `ConnectionType.SMART_WIFI` dalını bu istemciye
   yönlendirin.

## Testler ve Kod Kalitesi

- Katmanlar arasında net sorumluluk ayrımı (data/domain/ui).
- Tüm ViewModel'ler `StateFlow` ile tek yönlü veri akışı kullanır.
- `IrCodeEncoder` saf fonksiyonlardan oluşur; birim testi eklemek kolaydır
  (`app/src/test/...` altında JUnit testleri yazılabilir).

## Bilinen Sınırlamalar

- IR kod tablosu, tüm marka/model kombinasyonları için %100 doğrulanmış değildir
  (bkz. yukarıdaki genişletme rehberi).
- WiFi/akıllı cihaz protokolü placeholder durumundadır.
- Uygulama ikonları bu depoda vektörel (XML) olarak sağlanmıştır; isterseniz
  `ic_launcher_foreground.xml` dosyasını kendi logonuzla değiştirebilirsiniz.
