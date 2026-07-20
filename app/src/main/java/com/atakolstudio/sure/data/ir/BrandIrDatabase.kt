package com.atakolstudio.sure.data.ir

/**
 * Marka bazlı IR kod tabloları.
 *
 * ÖNEMLİ NOT: IR protokolleri (NEC, Samsung, Sony SIRC, RC5...) endüstri standardıdır
 * ve burada doğru şekilde uygulanmıştır (bkz. [IrCodeEncoder]). Ancak her üretici,
 * her model serisinde FARKLI "adres" (device code) ve "komut" (key code) değerleri
 * kullanabilir. Aşağıdaki tablo:
 *   - Samsung, LG, Sony, Toshiba, Sharp, Philips gibi markalarda en yaygın/klasik
 *     TV seri kodlarını (birçok üniversal kumandada da kullanılan "code 0000" seti) baz alır,
 *   - Daha az yaygın/az bilinen markalarda ise güvenli bir NEC "jenerik evrensel" kod
 *     seti kullanır (VERIFIED = false olarak işaretlenmiştir).
 *
 * Bir markanın kodları TV'nizde çalışmıyorsa:
 *   1) Ücretsiz bir "IR receiver / analyzer" uygulaması ile orijinal kumandanızın
 *      gerçek adres/komut baytlarını okuyun,
 *   2) Aşağıdaki ilgili [BrandIrCodeSet] bloğunu okuduğunuz değerlerle güncelleyin,
 *   3) `verified = true` yapın.
 *
 * Bu yapı sayesinde yeni bir marka eklemek sadece yeni bir [BrandIrCodeSet] tanımlamaktan
 * ibarettir — kodun geri kalanı hiçbir değişiklik gerektirmez.
 */

data class BrandIrCodeSet(
    val brandKey: String,
    val displayNameEn: String,
    val displayNameLocal: String, // Türkçe/Çince/yerel gösterim
    val protocol: IrProtocol,
    val verified: Boolean,
    /** address / extendedAddress bu markanın TÜM tuşları için ortaktır (protokol geregi) */
    val address: Int,
    val extendedAddress: Int? = null,
    /** Tuş -> komut baytı eşlemesi. Marka bu tuşu desteklemiyorsa haritada yer almaz. */
    val commands: Map<RemoteButton, Int>
)

object BrandIrDatabase {

    // Birçok jenerik/az bilinen marka için ortak, yaygın "universal NEC" tuş haritası.
    // Bu harita pek çok ucuz/OEM TV setinde ve üniversal kumandaların "generic" modunda kullanılır.
    private val genericNecCommands = mapOf(
        RemoteButton.POWER to 0x12,
        RemoteButton.VOLUME_UP to 0x1A,
        RemoteButton.VOLUME_DOWN to 0x1E,
        RemoteButton.MUTE to 0x10,
        RemoteButton.CHANNEL_UP to 0x00,
        RemoteButton.CHANNEL_DOWN to 0x01,
        RemoteButton.UP to 0x42,
        RemoteButton.DOWN to 0x43,
        RemoteButton.LEFT to 0x44,
        RemoteButton.RIGHT to 0x45,
        RemoteButton.OK to 0x1C,
        RemoteButton.MENU to 0x1B,
        RemoteButton.BACK to 0x17,
        RemoteButton.HOME to 0x2E,
        RemoteButton.INPUT to 0x0B,
        RemoteButton.NUM_0 to 0x08, RemoteButton.NUM_1 to 0x09, RemoteButton.NUM_2 to 0x0A,
        RemoteButton.NUM_3 to 0x0B, RemoteButton.NUM_4 to 0x0C, RemoteButton.NUM_5 to 0x0D,
        RemoteButton.NUM_6 to 0x0E, RemoteButton.NUM_7 to 0x0F, RemoteButton.NUM_8 to 0x10,
        RemoteButton.NUM_9 to 0x11
    )

    private fun genericBrand(key: String, en: String, local: String, address: Int) = BrandIrCodeSet(
        brandKey = key,
        displayNameEn = en,
        displayNameLocal = local,
        protocol = IrProtocol.NEC,
        verified = false,
        address = address,
        commands = genericNecCommands
    )

    val brands: List<BrandIrCodeSet> = listOf(
        // --- İyi bilinen / yaygın doğrulanmış kod setleri ---
        BrandIrCodeSet(
            brandKey = "samsung", displayNameEn = "Samsung", displayNameLocal = "삼성",
            protocol = IrProtocol.SAMSUNG, verified = true, address = 0x07,
            commands = mapOf(
                RemoteButton.POWER to 0x02,
                RemoteButton.VOLUME_UP to 0x07, RemoteButton.VOLUME_DOWN to 0x0B, RemoteButton.MUTE to 0x0F,
                RemoteButton.CHANNEL_UP to 0x12, RemoteButton.CHANNEL_DOWN to 0x10,
                RemoteButton.UP to 0x60, RemoteButton.DOWN to 0x61, RemoteButton.LEFT to 0x65,
                RemoteButton.RIGHT to 0x62, RemoteButton.OK to 0x68,
                RemoteButton.MENU to 0x1A, RemoteButton.BACK to 0x58, RemoteButton.HOME to 0x79,
                RemoteButton.INPUT to 0x0D,
                RemoteButton.RED to 0x6C, RemoteButton.GREEN to 0x14, RemoteButton.YELLOW to 0x15, RemoteButton.BLUE to 0x16,
                RemoteButton.NUM_0 to 0x11, RemoteButton.NUM_1 to 0x04, RemoteButton.NUM_2 to 0x05,
                RemoteButton.NUM_3 to 0x06, RemoteButton.NUM_4 to 0x08, RemoteButton.NUM_5 to 0x09,
                RemoteButton.NUM_6 to 0x0A, RemoteButton.NUM_7 to 0x0C, RemoteButton.NUM_8 to 0x0D,
                RemoteButton.NUM_9 to 0x0E
            )
        ),
        BrandIrCodeSet(
            brandKey = "lg", displayNameEn = "LG", displayNameLocal = "乐金",
            protocol = IrProtocol.NEC, verified = true, address = 0x04,
            commands = mapOf(
                RemoteButton.POWER to 0x08,
                RemoteButton.VOLUME_UP to 0x02, RemoteButton.VOLUME_DOWN to 0x03, RemoteButton.MUTE to 0x09,
                RemoteButton.CHANNEL_UP to 0x00, RemoteButton.CHANNEL_DOWN to 0x01,
                RemoteButton.UP to 0x40, RemoteButton.DOWN to 0x41, RemoteButton.LEFT to 0x07,
                RemoteButton.RIGHT to 0x06, RemoteButton.OK to 0x44,
                RemoteButton.MENU to 0x43, RemoteButton.BACK to 0x28, RemoteButton.HOME to 0x7E,
                RemoteButton.INPUT to 0x0B,
                RemoteButton.NUM_0 to 0x10, RemoteButton.NUM_1 to 0x11, RemoteButton.NUM_2 to 0x12,
                RemoteButton.NUM_3 to 0x13, RemoteButton.NUM_4 to 0x14, RemoteButton.NUM_5 to 0x15,
                RemoteButton.NUM_6 to 0x16, RemoteButton.NUM_7 to 0x17, RemoteButton.NUM_8 to 0x18,
                RemoteButton.NUM_9 to 0x19
            )
        ),
        BrandIrCodeSet(
            brandKey = "sony", displayNameEn = "Sony", displayNameLocal = "索尼",
            protocol = IrProtocol.SONY_SIRC12, verified = true, address = 0x01,
            commands = mapOf(
                RemoteButton.POWER to 0x15,
                RemoteButton.VOLUME_UP to 0x12, RemoteButton.VOLUME_DOWN to 0x13, RemoteButton.MUTE to 0x14,
                RemoteButton.CHANNEL_UP to 0x10, RemoteButton.CHANNEL_DOWN to 0x11,
                RemoteButton.UP to 0x74, RemoteButton.DOWN to 0x75, RemoteButton.LEFT to 0x34,
                RemoteButton.RIGHT to 0x33, RemoteButton.OK to 0x65,
                RemoteButton.MENU to 0x36, RemoteButton.BACK to 0x66, RemoteButton.HOME to 0x60,
                RemoteButton.INPUT to 0x25,
                RemoteButton.NUM_0 to 0x09, RemoteButton.NUM_1 to 0x00, RemoteButton.NUM_2 to 0x01,
                RemoteButton.NUM_3 to 0x02, RemoteButton.NUM_4 to 0x03, RemoteButton.NUM_5 to 0x04,
                RemoteButton.NUM_6 to 0x05, RemoteButton.NUM_7 to 0x06, RemoteButton.NUM_8 to 0x07,
                RemoteButton.NUM_9 to 0x08
            )
        ),
        BrandIrCodeSet(
            brandKey = "philips", displayNameEn = "Philips", displayNameLocal = "飞利浦",
            protocol = IrProtocol.RC5, verified = true, address = 0x00,
            commands = mapOf(
                RemoteButton.POWER to 0x0C,
                RemoteButton.VOLUME_UP to 0x10, RemoteButton.VOLUME_DOWN to 0x11, RemoteButton.MUTE to 0x0D,
                RemoteButton.CHANNEL_UP to 0x20, RemoteButton.CHANNEL_DOWN to 0x21,
                RemoteButton.UP to 0x50, RemoteButton.DOWN to 0x51, RemoteButton.LEFT to 0x55,
                RemoteButton.RIGHT to 0x56, RemoteButton.OK to 0x57,
                RemoteButton.MENU to 0x1F, RemoteButton.BACK to 0x52, RemoteButton.HOME to 0x7E,
                RemoteButton.INPUT to 0x22,
                RemoteButton.NUM_0 to 0x00, RemoteButton.NUM_1 to 0x01, RemoteButton.NUM_2 to 0x02,
                RemoteButton.NUM_3 to 0x03, RemoteButton.NUM_4 to 0x04, RemoteButton.NUM_5 to 0x05,
                RemoteButton.NUM_6 to 0x06, RemoteButton.NUM_7 to 0x07, RemoteButton.NUM_8 to 0x08,
                RemoteButton.NUM_9 to 0x09
            )
        ),
        BrandIrCodeSet(
            brandKey = "panasonic", displayNameEn = "Panasonic", displayNameLocal = "松下",
            protocol = IrProtocol.PANASONIC, verified = false, address = 0x40, extendedAddress = 0x04,
            commands = mapOf(
                RemoteButton.POWER to 0x3D,
                RemoteButton.VOLUME_UP to 0x20, RemoteButton.VOLUME_DOWN to 0x21, RemoteButton.MUTE to 0x24,
                RemoteButton.CHANNEL_UP to 0x22, RemoteButton.CHANNEL_DOWN to 0x23,
                RemoteButton.UP to 0x64, RemoteButton.DOWN to 0x65, RemoteButton.LEFT to 0x66,
                RemoteButton.RIGHT to 0x67, RemoteButton.OK to 0x68,
                RemoteButton.MENU to 0x60, RemoteButton.BACK to 0x72, RemoteButton.HOME to 0x6B,
                RemoteButton.INPUT to 0x5B
            )
        ),
        BrandIrCodeSet(
            brandKey = "toshiba", displayNameEn = "Toshiba", displayNameLocal = "东芝",
            protocol = IrProtocol.NEC, verified = false, address = 0x40,
            commands = genericNecCommands
        ),
        BrandIrCodeSet(
            brandKey = "sharp", displayNameEn = "Sharp", displayNameLocal = "夏普",
            protocol = IrProtocol.NEC, verified = false, address = 0x5A,
            commands = genericNecCommands
        ),
        BrandIrCodeSet(
            brandKey = "vestel", displayNameEn = "Vestel", displayNameLocal = "Vestel",
            protocol = IrProtocol.NEC, verified = false, address = 0x53,
            commands = genericNecCommands
        ),

        // --- Genel/az bilinen markalar (jenerik NEC evrensel kod seti; doğrulama önerilir) ---
        genericBrand("insignia", "Insignia", "Insignia", 0x4F),
        genericBrand("jvc", "JVC", "JVC", 0x30),
        genericBrand("vizio", "Vizio", "Vizio", 0x51),
        genericBrand("telefunken", "Telefunken", "Telefunken", 0x1E),
        genericBrand("grundig", "Grundig", "Grundig", 0x1E),
        genericBrand("aoc", "AOC", "AOC", 0x6B),
        genericBrand("hisense", "Hisense", "海信", 0x3C),
        genericBrand("tcl", "TCL", "TCL", 0x80),
        genericBrand("thomson", "Thomson", "Thomson", 0x5B),
        genericBrand("changhong", "Changhong", "长虹", 0x6E),
        genericBrand("konka", "Konka", "康佳", 0x33),
        genericBrand("skyworth", "Skyworth", "创维", 0x6F),
        genericBrand("regal", "Regal", "Regal", 0x53),
        genericBrand("arcelik", "Arçelik", "Arçelik", 0x53),
        genericBrand("beko", "Beko", "Beko", 0x53),
        genericBrand("polaroid", "Polaroid", "Polaroid", 0x4B),
        genericBrand("sanyo", "Sanyo", "三洋", 0x3E)
    )

    fun findByKey(key: String): BrandIrCodeSet? = brands.find { it.brandKey == key }

    fun toIrCommand(brand: BrandIrCodeSet, button: RemoteButton): IrCommand? {
        val commandByte = brand.commands[button] ?: return null
        return IrCommand(
            protocol = brand.protocol,
            address = brand.address,
            command = commandByte,
            extendedAddress = brand.extendedAddress
        )
    }
}
