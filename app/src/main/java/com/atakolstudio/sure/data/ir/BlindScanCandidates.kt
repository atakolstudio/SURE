package com.atakolstudio.sure.data.ir

/**
 * "Kör Tarama" (blind scan) aday üreticisi.
 *
 * Marka veritabanındaki (BrandIrDatabase) ~25 marka, YALNIZCA bilinen/isimli
 * cihazları kapsar. Ama piyasada isimsiz/OEM/Çin üretimi TV, klima ve benzeri
 * birçok cihaz da aslında NEC protokolünü kullanır — sadece hangi "adres" ve
 * "komut" baytlarını kullandıkları farklıdır.
 *
 * Bu nesne, NEC protokolü için geniş bir adres × komut kombinasyon listesi
 * üretir. Böylece "Kod Tarama" özelliği yalnızca veritabanındaki markaları değil,
 * veritabanında HİÇ tanımlı olmayan cihazları da (gerçek evrensel kumandaların
 * yaptığı gibi) bulabilir.
 *
 * Not: Bu, gerçek/kesin bir kod kütüphanesi değil, NEC protokolünün olası
 * adres/komut alanının sistematik bir taramasıdır. Bu yüzden %100 garanti
 * değildir ama pratikte NEC tabanlı çoğu cihazı zamanla bulur.
 */
object BlindScanCandidates {

    /**
     * Farklı üreticilerin gerçek POWER komut baytları (bkz. BrandIrDatabase) + literatürde
     * sık görülen ek değerler. Adres taramasında her adres bu komutların hepsiyle denenir.
     */
    private val commonPowerCommands: List<Int> = listOf(
        0x02, 0x08, 0x0C, 0x10, 0x12, 0x15, 0x1A, 0x1C, 0x1E, 0x3D, 0x40, 0x5C
    ).distinct()

    /**
     * NEC protokolü için adres × komut taraması üretir.
     * @param addressRange Denenecek adres aralığı (varsayılan: 0-255, tüm bayt alanı).
     */
    fun generateNecSweep(addressRange: IntRange = 0..255): List<BrandIrCodeSet> {
        val result = ArrayList<BrandIrCodeSet>(addressRange.count() * commonPowerCommands.size)
        for (address in addressRange) {
            for (command in commonPowerCommands) {
                result += BrandIrCodeSet(
                    brandKey = "blind_nec_${address}_$command",
                    displayNameEn = "Bilinmeyen Cihaz",
                    displayNameLocal = "NEC · Adres 0x%02X · Komut 0x%02X".format(address, command),
                    protocol = IrProtocol.NEC,
                    verified = false,
                    address = address,
                    commands = mapOf(RemoteButton.POWER to command)
                )
            }
        }
        return result
    }

    /**
     * Sony SIRC (12-bit) için adres × komut taraması. Sony cihazlarda adres alanı
     * dar olduğundan (5 bit) tüm alanı taramak pratiktir.
     */
    fun generateSonySweep(): List<BrandIrCodeSet> {
        val result = ArrayList<BrandIrCodeSet>()
        for (address in 0..31) {
            for (command in commonPowerCommands.filter { it <= 0x7F }) {
                result += BrandIrCodeSet(
                    brandKey = "blind_sony_${address}_$command",
                    displayNameEn = "Bilinmeyen Cihaz",
                    displayNameLocal = "Sony SIRC · Adres 0x%02X · Komut 0x%02X".format(address, command),
                    protocol = IrProtocol.SONY_SIRC12,
                    verified = false,
                    address = address,
                    commands = mapOf(RemoteButton.POWER to command)
                )
            }
        }
        return result
    }

    /**
     * Tam kör tarama listesi: önce (daha olası) düşük adresli NEC kodları, ardından
     * Sony taraması, ardından NEC'in geri kalan (yüksek) adresleri. Bu sıralama,
     * en yaygın adreslerin en erken denenmesini sağlar.
     */
    fun generateFullBlindScan(): List<BrandIrCodeSet> {
        return generateNecSweep(0..127) + generateSonySweep() + generateNecSweep(128..255)
    }
}
