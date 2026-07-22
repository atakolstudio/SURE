package com.atakolstudio.sure.data.ir

/**
 * Desteklenen kızılötesi (IR) protokolleri.
 * Her protokolün kendine özgü taşıyıcı frekansı ve zamanlama kuralları vardır.
 */
enum class IrProtocol(val carrierFrequencyHz: Int) {
    NEC(38000),          // LG, Toshiba, Sharp, Vizio, Hisense, TCL, birçok jenerik marka
    SAMSUNG(38000),      // Samsung (NEC'e çok benzer, farklı header)
    SONY_SIRC12(40000),  // Sony (12-bit)
    SONY_SIRC15(40000),  // Sony (15-bit, genişletilmiş)
    SONY_SIRC20(40000),  // Sony (20-bit, genişletilmiş)
    RC5(36000),          // Philips, Grundig, Telefunken, Marantz
    RC6(36000),          // Philips (yeni nesil), Panasonic bazı modeller
    PANASONIC(37000),    // Panasonic (Kaseikyo ailesi)
    JVC(38000)           // JVC ve türevleri (16 bit, tümleç yok)
}

/**
 * Tek bir IR komutunu tanımlar: protokol + adres (cihaz kodu) + komut (tuş kodu).
 * Bu üçlüden gerçek zamanlama darbeleri [IrCodeEncoder] tarafından üretilir.
 */
data class IrCommand(
    val protocol: IrProtocol,
    val address: Int,
    val command: Int,
    /** Bazı markalarda adres 16 bit genişletilmiş olabilir (ör. NEC extended). */
    val extendedAddress: Int? = null
)
