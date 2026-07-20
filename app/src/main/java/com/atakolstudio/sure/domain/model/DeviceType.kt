package com.atakolstudio.sure.domain.model

/**
 * Uzaktan kumanda ile kontrol edilebilecek cihaz türleri.
 */
enum class DeviceType(val displayNameTr: String) {
    TV("Televizyon"),
    SET_TOP_BOX("Set Üstü / Kablo Kutusu"),
    AC("Klima"),
    AV_RECEIVER("AV Alıcısı / Ses Çubuğu"),
    STREAMING_MEDIA("Ortam Yayıncısı"),
    HOME_AUTOMATION("Ev Otomasyonu"),
    DISC_PLAYER("Disk Oynatıcı"),
    PROJECTOR("Projektör")
}

/**
 * Cihaza bağlanma yöntemi.
 */
enum class ConnectionType(val displayNameTr: String) {
    SMART_WIFI("Akıllı Cihaz (WiFi)"),
    TRADITIONAL_IR("Geleneksel Aygıt (IR)")
}
