package com.atakolstudio.sure.data.ir

/**
 * Marka bazli IR kod tablolari.
 *
 * Buradaki dogrulanmis (verified = true) kod setlerinin cogu, LIRC (Linux Infrared
 * Remote Control) projesinin acik kaynak "remotes" veritabanindan
 * (https://lirc.sourceforge.net/remotes/, ayna: github.com/probonopd/lirc-remotes)
 * turetilmistir. Bu veritabani, gercek kumandalarin bir kizilotesi alicisiyla
 * okunup analiz edilmesiyle olusturulmus, on yillardir kullanilan bir kaynaktir.
 *
 * ONEMLI NOT: Her uretici, her model serisinde FARKLI "adres" (device code) ve
 * "komut" (key code) degerleri kullanabilir. Asagidaki tablo:
 *   - LIRC veritabaninda gercek karsiligi bulunan markalarda (Samsung, LG, Sony,
 *     Philips, Toshiba, Sharp, Vestel, Beko, JVC, Telefunken, Grundig, Thomson,
 *     Daewoo, Akai, RCA, Orion, Polaroid, Goldstar, Insignia, AOC, Vizio, Sanyo)
 *     gercek, dogrulanmis kod setlerini kullanir,
 *   - LIRC veritabaninda karsiligi bulunamayan/az bilinen markalarda ise guvenli
 *     bir NEC "jenerik evrensel" kod seti kullanir (verified = false).
 *
 * Bir markanin kodlari TV'nizde calismiyorsa (ayni marka icinde bile onlarca farkli
 * kumanda modeli olabilir):
 *   1) Ucretsiz bir "IR receiver / analyzer" uygulamasiyla orijinal kumandanizin
 *      gercek adres/komut baytlarini okuyun,
 *   2) Asagidaki ilgili [BrandIrCodeSet] blogunu okudugunuz degerlerle guncelleyin,
 *   3) `verified = true` yapin.
 *   (Alternatif olarak uygulama icindeki "Markami Bilmiyorum -> Manuel Bul"
 *   ozelligiyle de kendi cihazinizin kodunu bulup kaydedebilirsiniz.)
 *
 * Bu yapi sayesinde yeni bir marka eklemek sadece yeni bir [BrandIrCodeSet] tanimlamaktan
 * ibarettir; kodun geri kalani hicbir degisiklik gerektirmez.
 */

data class BrandIrCodeSet(
    val brandKey: String,
    val displayNameEn: String,
    val displayNameLocal: String, // Turkce/Cince/yerel gosterim
    val protocol: IrProtocol,
    val verified: Boolean,
    /** address / extendedAddress bu markanin TUM tuslari icin ortaktir (protokol geregi) */
    val address: Int,
    val extendedAddress: Int? = null,
    /** Tus -> komut bayti eslemesi. Marka bu tusu desteklemiyorsa haritada yer almaz. */
    val commands: Map<RemoteButton, Int>
)

object BrandIrDatabase {

    // Birçok jenerik/az bilinen marka için ortak, yaygın "universal NEC" tuş haritası.
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
        // --- Cok yaygin markalar (elle dogrulanmis / literatur kaynakli) ---
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

        // --- LIRC veritabanindan alinmis, gercek/dogrulanmis kod setleri ---
        // Kaynak: https://github.com/probonopd/lirc-remotes (LIRC remotes database aynasi)
        BrandIrCodeSet(
            brandKey = "toshiba", displayNameEn = "Toshiba", displayNameLocal = "东芝",
            protocol = IrProtocol.NEC, verified = true, address = 0x40,
            commands = mapOf(
                RemoteButton.BACK to 0x43, RemoteButton.BLUE to 0x4B, RemoteButton.CHANNEL_DOWN to 0x1F,
                RemoteButton.CHANNEL_UP to 0x1B, RemoteButton.DOWN to 0x1D, RemoteButton.GREEN to 0x49,
                RemoteButton.LEFT to 0x42, RemoteButton.MENU to 0x5B, RemoteButton.MUTE to 0x10,
                RemoteButton.NUM_0 to 0x0, RemoteButton.NUM_1 to 0x1, RemoteButton.NUM_2 to 0x2,
                RemoteButton.NUM_3 to 0x3, RemoteButton.NUM_4 to 0x4, RemoteButton.NUM_5 to 0x5,
                RemoteButton.NUM_6 to 0x6, RemoteButton.NUM_7 to 0x7, RemoteButton.NUM_8 to 0x8,
                RemoteButton.NUM_9 to 0x9, RemoteButton.OK to 0x21, RemoteButton.POWER to 0x12,
                RemoteButton.RED to 0x48, RemoteButton.RIGHT to 0x40, RemoteButton.UP to 0x19,
                RemoteButton.VOLUME_DOWN to 0x1E, RemoteButton.VOLUME_UP to 0x1A, RemoteButton.YELLOW to 0x4A
            )
        ),
        BrandIrCodeSet(
            brandKey = "sharp", displayNameEn = "Sharp", displayNameLocal = "夏普",
            protocol = IrProtocol.NEC, verified = true, address = 0x0, extendedAddress = 189,
            commands = mapOf(
                RemoteButton.BLUE to 0x4F, RemoteButton.CHANNEL_DOWN to 0x1C, RemoteButton.CHANNEL_UP to 0x18,
                RemoteButton.DOWN to 0xF, RemoteButton.GREEN to 0x4D, RemoteButton.LEFT to 0x49,
                RemoteButton.MENU to 0xA, RemoteButton.MUTE to 0x4,
                RemoteButton.NUM_0 to 0x41, RemoteButton.NUM_1 to 0x15, RemoteButton.NUM_2 to 0x16,
                RemoteButton.NUM_3 to 0x17, RemoteButton.NUM_4 to 0x19, RemoteButton.NUM_5 to 0x1A,
                RemoteButton.NUM_6 to 0x1B, RemoteButton.NUM_7 to 0x1D, RemoteButton.NUM_8 to 0x1E,
                RemoteButton.NUM_9 to 0x1F, RemoteButton.OK to 0xD, RemoteButton.POWER to 0x1,
                RemoteButton.RED to 0x4C, RemoteButton.RIGHT to 0x4A, RemoteButton.UP to 0xB,
                RemoteButton.VOLUME_DOWN to 0x10, RemoteButton.VOLUME_UP to 0xC, RemoteButton.YELLOW to 0x4E
            )
        ),
        BrandIrCodeSet(
            brandKey = "vestel", displayNameEn = "Vestel", displayNameLocal = "Vestel",
            protocol = IrProtocol.RC5, verified = true, address = 0x1D,
            commands = mapOf(
                RemoteButton.BACK to 0x2C, RemoteButton.BLUE to 0x34, RemoteButton.CHANNEL_DOWN to 0x20,
                RemoteButton.CHANNEL_UP to 0x21, RemoteButton.GREEN to 0x32, RemoteButton.INPUT to 0x1,
                RemoteButton.LEFT to 0x10, RemoteButton.MENU to 0xE,
                RemoteButton.NUM_0 to 0x7, RemoteButton.NUM_1 to 0x12, RemoteButton.NUM_2 to 0x3,
                RemoteButton.NUM_3 to 0x2, RemoteButton.NUM_4 to 0xA, RemoteButton.NUM_5 to 0x6,
                RemoteButton.NUM_6 to 0x5, RemoteButton.NUM_7 to 0x0, RemoteButton.NUM_8 to 0x9,
                RemoteButton.NUM_9 to 0x8, RemoteButton.OK to 0x22, RemoteButton.PLAY_PAUSE to 0x2E,
                RemoteButton.POWER to 0xD, RemoteButton.RED to 0x36, RemoteButton.RIGHT to 0x11,
                RemoteButton.STOP to 0x2F, RemoteButton.YELLOW to 0x38
                // Not: Bu modelde ses tuşları farklı bir RC5 adresi (0x00) kullanıyor;
                // tutarlılık için burada dahil edilmedi. Gerekirse "Manuel Bul" ile eklenebilir.
            )
        ),
        BrandIrCodeSet(
            brandKey = "insignia", displayNameEn = "Insignia", displayNameLocal = "Insignia",
            protocol = IrProtocol.NEC, verified = true, address = 0x43, extendedAddress = 71,
            commands = mapOf(
                RemoteButton.DOWN to 0x14, RemoteButton.LEFT to 0x12, RemoteButton.MENU to 0xA,
                RemoteButton.MUTE to 0x1C,
                RemoteButton.NUM_0 to 0x0, RemoteButton.NUM_1 to 0x1, RemoteButton.NUM_2 to 0x2,
                RemoteButton.NUM_3 to 0x3, RemoteButton.NUM_4 to 0x4, RemoteButton.NUM_5 to 0x5,
                RemoteButton.NUM_6 to 0x6, RemoteButton.NUM_7 to 0x7, RemoteButton.NUM_8 to 0x8,
                RemoteButton.NUM_9 to 0x9, RemoteButton.OK to 0x10, RemoteButton.PLAY_PAUSE to 0xE,
                RemoteButton.POWER to 0x2F, RemoteButton.RIGHT to 0x13, RemoteButton.SETTINGS to 0x26,
                RemoteButton.STOP to 0xF, RemoteButton.UP to 0x11, RemoteButton.VOLUME_DOWN to 0xD,
                RemoteButton.VOLUME_UP to 0xC
            )
        ),
        BrandIrCodeSet(
            brandKey = "jvc", displayNameEn = "JVC", displayNameLocal = "JVC",
            protocol = IrProtocol.JVC, verified = true, address = 0x3,
            commands = mapOf(
                RemoteButton.DOWN to 0x18, RemoteButton.MUTE to 0x1C,
                RemoteButton.NUM_0 to 0x20, RemoteButton.NUM_1 to 0x21, RemoteButton.NUM_2 to 0x22,
                RemoteButton.NUM_3 to 0x23, RemoteButton.NUM_4 to 0x24, RemoteButton.NUM_5 to 0x25,
                RemoteButton.NUM_6 to 0x26, RemoteButton.NUM_7 to 0x27, RemoteButton.NUM_8 to 0x28,
                RemoteButton.NUM_9 to 0x29, RemoteButton.POWER to 0x17, RemoteButton.UP to 0x19,
                RemoteButton.VOLUME_DOWN to 0x1F
            )
        ),
        BrandIrCodeSet(
            brandKey = "telefunken", displayNameEn = "Telefunken", displayNameLocal = "Telefunken",
            protocol = IrProtocol.JVC, verified = true, address = 0x43,
            commands = mapOf(
                RemoteButton.FAST_FORWARD to 0x6, RemoteButton.GREEN to 0x15, RemoteButton.MENU to 0x1E,
                RemoteButton.NUM_0 to 0x33, RemoteButton.NUM_1 to 0x21, RemoteButton.NUM_2 to 0x22,
                RemoteButton.NUM_3 to 0x23, RemoteButton.NUM_4 to 0x24, RemoteButton.NUM_5 to 0x25,
                RemoteButton.NUM_6 to 0x26, RemoteButton.NUM_7 to 0x27, RemoteButton.NUM_8 to 0x28,
                RemoteButton.NUM_9 to 0x29, RemoteButton.OK to 0x3C, RemoteButton.PLAY_PAUSE to 0xC,
                RemoteButton.POWER to 0xB, RemoteButton.REWIND to 0x7, RemoteButton.STOP to 0x3
            )
        ),
        BrandIrCodeSet(
            brandKey = "grundig", displayNameEn = "Grundig", displayNameLocal = "Grundig",
            protocol = IrProtocol.NEC, verified = true, address = 0x80,
            commands = mapOf(
                RemoteButton.MENU to 0xF,
                RemoteButton.NUM_0 to 0x41, RemoteButton.NUM_1 to 0x8, RemoteButton.NUM_2 to 0x9,
                RemoteButton.NUM_3 to 0xA, RemoteButton.NUM_4 to 0x10, RemoteButton.NUM_5 to 0x11,
                RemoteButton.NUM_6 to 0x12, RemoteButton.NUM_7 to 0x18, RemoteButton.NUM_8 to 0x19,
                RemoteButton.NUM_9 to 0x1A, RemoteButton.OK to 0x13, RemoteButton.PLAY_PAUSE to 0x49,
                RemoteButton.POWER to 0x0, RemoteButton.SETTINGS to 0xD, RemoteButton.VOLUME_DOWN to 0x45,
                RemoteButton.VOLUME_UP to 0x1D
            )
        ),
        BrandIrCodeSet(
            brandKey = "aoc", displayNameEn = "AOC", displayNameLocal = "AOC",
            protocol = IrProtocol.NEC, verified = true, address = 0x0, extendedAddress = 189,
            commands = mapOf(
                RemoteButton.DOWN to 0x1C, RemoteButton.LEFT to 0x10, RemoteButton.MENU to 0xA,
                RemoteButton.MUTE to 0x4,
                RemoteButton.NUM_0 to 0x41, RemoteButton.NUM_1 to 0x15, RemoteButton.NUM_2 to 0x16,
                RemoteButton.NUM_3 to 0x17, RemoteButton.NUM_4 to 0x19, RemoteButton.NUM_5 to 0x1A,
                RemoteButton.NUM_6 to 0x1B, RemoteButton.NUM_7 to 0x1D, RemoteButton.NUM_8 to 0x1E,
                RemoteButton.NUM_9 to 0x1F, RemoteButton.POWER to 0x1, RemoteButton.RIGHT to 0xC,
                RemoteButton.UP to 0x18
            )
        ),
        BrandIrCodeSet(
            brandKey = "sanyo", displayNameEn = "Sanyo", displayNameLocal = "三洋",
            protocol = IrProtocol.NEC, verified = true, address = 0x38,
            commands = mapOf(
                RemoteButton.BLUE to 0x4C, RemoteButton.DOWN to 0xD, RemoteButton.GREEN to 0x4A,
                RemoteButton.LEFT to 0x17, RemoteButton.MENU to 0x18, RemoteButton.MUTE to 0x15,
                RemoteButton.NUM_0 to 0x0, RemoteButton.NUM_1 to 0x1, RemoteButton.NUM_2 to 0x2,
                RemoteButton.NUM_3 to 0x3, RemoteButton.NUM_4 to 0x4, RemoteButton.NUM_5 to 0x5,
                RemoteButton.NUM_6 to 0x6, RemoteButton.NUM_7 to 0x7, RemoteButton.NUM_8 to 0x8,
                RemoteButton.NUM_9 to 0x9, RemoteButton.OK to 0x1A, RemoteButton.POWER to 0x1C,
                RemoteButton.RED to 0x49, RemoteButton.RIGHT to 0x16, RemoteButton.UP to 0xC,
                RemoteButton.YELLOW to 0x4B
            )
        ),
        BrandIrCodeSet(
            brandKey = "vizio", displayNameEn = "Vizio", displayNameLocal = "Vizio",
            protocol = IrProtocol.NEC, verified = true, address = 0x4,
            commands = mapOf(
                RemoteButton.BACK to 0x49, RemoteButton.CHANNEL_UP to 0x0, RemoteButton.DOWN to 0x46,
                RemoteButton.INPUT to 0xD6, RemoteButton.LEFT to 0x47, RemoteButton.MENU to 0x43,
                RemoteButton.MUTE to 0x9,
                RemoteButton.NUM_0 to 0x10, RemoteButton.NUM_1 to 0x11, RemoteButton.NUM_2 to 0x12,
                RemoteButton.NUM_3 to 0x13, RemoteButton.NUM_4 to 0x14, RemoteButton.NUM_5 to 0x15,
                RemoteButton.NUM_6 to 0x16, RemoteButton.NUM_7 to 0x17, RemoteButton.NUM_8 to 0x18,
                RemoteButton.NUM_9 to 0x19, RemoteButton.OK to 0x44, RemoteButton.POWER to 0x8,
                RemoteButton.RIGHT to 0x48, RemoteButton.UP to 0x45, RemoteButton.VOLUME_DOWN to 0x3,
                RemoteButton.VOLUME_UP to 0x2
            )
        ),
        BrandIrCodeSet(
            brandKey = "thomson", displayNameEn = "Thomson", displayNameLocal = "Thomson",
            protocol = IrProtocol.RC5, verified = true, address = 0x0,
            commands = mapOf(
                RemoteButton.BACK to 0x2E, RemoteButton.BLUE to 0x1E, RemoteButton.CHANNEL_DOWN to 0x21,
                RemoteButton.CHANNEL_UP to 0x20, RemoteButton.GREEN to 0x2C, RemoteButton.MENU to 0xE,
                RemoteButton.MUTE to 0x23,
                RemoteButton.NUM_0 to 0x0, RemoteButton.NUM_1 to 0x1, RemoteButton.NUM_2 to 0x2,
                RemoteButton.NUM_3 to 0x3, RemoteButton.NUM_4 to 0x4, RemoteButton.NUM_5 to 0x5,
                RemoteButton.NUM_6 to 0x6, RemoteButton.NUM_7 to 0x7, RemoteButton.NUM_8 to 0x8,
                RemoteButton.NUM_9 to 0x9, RemoteButton.OK to 0xD, RemoteButton.PLAY_PAUSE to 0x32,
                RemoteButton.POWER to 0xC, RemoteButton.RED to 0x2D, RemoteButton.REWIND to 0x37,
                RemoteButton.STOP to 0x36, RemoteButton.VOLUME_DOWN to 0x11, RemoteButton.VOLUME_UP to 0x10,
                RemoteButton.YELLOW to 0x2B
            )
        ),
        BrandIrCodeSet(
            brandKey = "daewoo", displayNameEn = "Daewoo", displayNameLocal = "Daewoo",
            protocol = IrProtocol.NEC, verified = true, address = 0x20,
            commands = mapOf(
                RemoteButton.DOWN to 0x1D, RemoteButton.LEFT to 0x1C, RemoteButton.MENU to 0x42,
                RemoteButton.MUTE to 0x51,
                RemoteButton.NUM_0 to 0x49, RemoteButton.NUM_1 to 0x13, RemoteButton.NUM_2 to 0x10,
                RemoteButton.NUM_3 to 0x11, RemoteButton.NUM_4 to 0xF, RemoteButton.NUM_5 to 0xC,
                RemoteButton.NUM_6 to 0xD, RemoteButton.NUM_7 to 0xB, RemoteButton.NUM_8 to 0x8,
                RemoteButton.NUM_9 to 0x9, RemoteButton.PLAY_PAUSE to 0x5C, RemoteButton.POWER to 0xE,
                RemoteButton.RIGHT to 0x48, RemoteButton.SETTINGS to 0x2, RemoteButton.STOP to 0x5D,
                RemoteButton.UP to 0x44, RemoteButton.VOLUME_DOWN to 0x15, RemoteButton.VOLUME_UP to 0x55
            )
        ),
        BrandIrCodeSet(
            brandKey = "akai", displayNameEn = "Akai", displayNameLocal = "Akai",
            protocol = IrProtocol.NEC, verified = true, address = 0x89, extendedAddress = 119,
            commands = mapOf(
                RemoteButton.BLUE to 0xC, RemoteButton.CHANNEL_DOWN to 0xE, RemoteButton.CHANNEL_UP to 0xD,
                RemoteButton.DOWN to 0x5D, RemoteButton.FAST_FORWARD to 0x1, RemoteButton.GREEN to 0x5,
                RemoteButton.LEFT to 0x59, RemoteButton.MENU to 0x4F,
                RemoteButton.NUM_0 to 0x49, RemoteButton.NUM_1 to 0x40, RemoteButton.NUM_2 to 0x41,
                RemoteButton.NUM_3 to 0x42, RemoteButton.NUM_4 to 0x43, RemoteButton.NUM_5 to 0x44,
                RemoteButton.NUM_6 to 0x45, RemoteButton.NUM_7 to 0x46, RemoteButton.NUM_8 to 0x47,
                RemoteButton.NUM_9 to 0x48, RemoteButton.OK to 0x50, RemoteButton.PLAY_PAUSE to 0x6,
                RemoteButton.POWER to 0x13, RemoteButton.RED to 0xB, RemoteButton.REWIND to 0x2,
                RemoteButton.RIGHT to 0x58, RemoteButton.STOP to 0x3, RemoteButton.UP to 0x5C,
                RemoteButton.YELLOW to 0x4
            )
        ),
        BrandIrCodeSet(
            brandKey = "rca", displayNameEn = "RCA", displayNameLocal = "RCA",
            protocol = IrProtocol.NEC, verified = true, address = 0x87, extendedAddress = 94,
            commands = mapOf(
                RemoteButton.BLUE to 0x19, RemoteButton.CHANNEL_DOWN to 0x1A, RemoteButton.CHANNEL_UP to 0x10,
                RemoteButton.DOWN to 0xC, RemoteButton.GREEN to 0x17, RemoteButton.LEFT to 0x1B,
                RemoteButton.MENU to 0x23, RemoteButton.MUTE to 0x1F,
                RemoteButton.NUM_0 to 0x0, RemoteButton.NUM_1 to 0x1, RemoteButton.NUM_2 to 0x2,
                RemoteButton.NUM_3 to 0x3, RemoteButton.NUM_4 to 0x4, RemoteButton.NUM_5 to 0x5,
                RemoteButton.NUM_6 to 0x6, RemoteButton.NUM_7 to 0x7, RemoteButton.NUM_8 to 0x8,
                RemoteButton.NUM_9 to 0x9, RemoteButton.OK to 0x15, RemoteButton.POWER to 0x12,
                RemoteButton.RED to 0x16, RemoteButton.UP to 0xB, RemoteButton.VOLUME_DOWN to 0xE,
                RemoteButton.VOLUME_UP to 0xD, RemoteButton.YELLOW to 0x18
            )
        ),
        BrandIrCodeSet(
            brandKey = "orion", displayNameEn = "Orion", displayNameLocal = "Orion",
            protocol = IrProtocol.NEC, verified = true, address = 0x0,
            commands = mapOf(
                RemoteButton.BACK to 0x9, RemoteButton.CHANNEL_DOWN to 0xA, RemoteButton.CHANNEL_UP to 0x5,
                RemoteButton.MENU to 0x11, RemoteButton.MUTE to 0xE,
                RemoteButton.NUM_0 to 0x44, RemoteButton.NUM_1 to 0x16, RemoteButton.NUM_2 to 0x1A,
                RemoteButton.NUM_3 to 0x1E, RemoteButton.NUM_4 to 0x1B, RemoteButton.NUM_5 to 0x17,
                RemoteButton.NUM_6 to 0x13, RemoteButton.NUM_7 to 0xB, RemoteButton.NUM_8 to 0x7,
                RemoteButton.NUM_9 to 0x3, RemoteButton.OK to 0x2, RemoteButton.POWER to 0x0,
                RemoteButton.VOLUME_DOWN to 0x1, RemoteButton.VOLUME_UP to 0x6
            )
        ),
        BrandIrCodeSet(
            brandKey = "polaroid", displayNameEn = "Polaroid", displayNameLocal = "Polaroid",
            protocol = IrProtocol.NEC, verified = true, address = 0x20,
            commands = mapOf(
                RemoteButton.DOWN to 0x1D, RemoteButton.LEFT to 0x1C, RemoteButton.MENU to 0x42,
                RemoteButton.MUTE to 0x51,
                RemoteButton.NUM_0 to 0x49, RemoteButton.NUM_1 to 0x13, RemoteButton.NUM_2 to 0x10,
                RemoteButton.NUM_3 to 0x11, RemoteButton.NUM_4 to 0xF, RemoteButton.NUM_5 to 0xC,
                RemoteButton.NUM_6 to 0xD, RemoteButton.NUM_7 to 0xB, RemoteButton.NUM_8 to 0x8,
                RemoteButton.NUM_9 to 0x9, RemoteButton.PLAY_PAUSE to 0x5C, RemoteButton.POWER to 0xE,
                RemoteButton.RIGHT to 0x48, RemoteButton.SETTINGS to 0x2, RemoteButton.STOP to 0x5D,
                RemoteButton.UP to 0x44, RemoteButton.VOLUME_DOWN to 0x4B, RemoteButton.VOLUME_UP to 0x4F
            )
        ),
        BrandIrCodeSet(
            brandKey = "goldstar", displayNameEn = "Goldstar", displayNameLocal = "골드스타 (eski LG)",
            protocol = IrProtocol.NEC, verified = true, address = 0x6E,
            commands = mapOf(
                RemoteButton.BACK to 0x4C, RemoteButton.DOWN to 0x89, RemoteButton.FAST_FORWARD to 0x3,
                RemoteButton.LEFT to 0x83, RemoteButton.MENU to 0x16,
                RemoteButton.NUM_0 to 0x4, RemoteButton.NUM_1 to 0x5, RemoteButton.NUM_2 to 0x6,
                RemoteButton.NUM_3 to 0x7, RemoteButton.NUM_4 to 0xC, RemoteButton.NUM_5 to 0xD,
                RemoteButton.NUM_6 to 0xE, RemoteButton.NUM_7 to 0xF, RemoteButton.NUM_8 to 0x1C,
                RemoteButton.NUM_9 to 0x1D, RemoteButton.OK to 0x8E, RemoteButton.PLAY_PAUSE to 0x8,
                RemoteButton.POWER to 0x14, RemoteButton.RIGHT to 0x90, RemoteButton.STOP to 0x1,
                RemoteButton.UP to 0x82
            )
        ),
        BrandIrCodeSet(
            brandKey = "beko", displayNameEn = "Beko", displayNameLocal = "Beko",
            protocol = IrProtocol.RC5, verified = true, address = 0x0,
            commands = mapOf(
                RemoteButton.BLUE to 0x33, RemoteButton.GREEN to 0x2C, RemoteButton.MENU to 0x29,
                RemoteButton.MUTE to 0x2B,
                RemoteButton.NUM_0 to 0x10, RemoteButton.NUM_1 to 0x11, RemoteButton.NUM_2 to 0x12,
                RemoteButton.NUM_3 to 0x13, RemoteButton.NUM_4 to 0x14, RemoteButton.NUM_5 to 0x15,
                RemoteButton.NUM_6 to 0x16, RemoteButton.NUM_7 to 0x17, RemoteButton.NUM_8 to 0x18,
                RemoteButton.NUM_9 to 0x19, RemoteButton.OK to 0x28, RemoteButton.POWER to 0x20,
                RemoteButton.RED to 0x26, RemoteButton.STOP to 0xC, RemoteButton.VOLUME_UP to 0x24,
                RemoteButton.YELLOW to 0x2E
            )
        ),

        // --- LIRC veritabaninda dogrudan karsiligi bulunamayan / az bilinen markalar
        //     (jenerik NEC evrensel kod seti; dogrulama onerilir) ---
        genericBrand("hisense", "Hisense", "海信", 0x3C),
        genericBrand("tcl", "TCL", "TCL", 0x80),
        genericBrand("changhong", "Changhong", "长虹", 0x6E),
        genericBrand("konka", "Konka", "康佳", 0x33),
        genericBrand("skyworth", "Skyworth", "创维", 0x6F),
        // Not: Regal ve Arçelik, Beko ile aynı üretici grubuna (Arçelik A.Ş.) aittir ve
        // çoğu zaman aynı/benzer kumanda donanımını paylaşır. Bu yüzden Beko'nun
        // doğrulanmış RC5 kodu burada temel alınmıştır, ancak marka-özel doğrulama
        // yapılmadığı için verified=false bırakılmıştır.
        BrandIrCodeSet(
            brandKey = "regal", displayNameEn = "Regal", displayNameLocal = "Regal",
            protocol = IrProtocol.RC5, verified = false, address = 0x0,
            commands = mapOf(
                RemoteButton.BLUE to 0x33, RemoteButton.GREEN to 0x2C, RemoteButton.MENU to 0x29,
                RemoteButton.MUTE to 0x2B, RemoteButton.OK to 0x28, RemoteButton.POWER to 0x20,
                RemoteButton.RED to 0x26, RemoteButton.VOLUME_UP to 0x24, RemoteButton.YELLOW to 0x2E,
                RemoteButton.NUM_0 to 0x10, RemoteButton.NUM_1 to 0x11, RemoteButton.NUM_2 to 0x12,
                RemoteButton.NUM_3 to 0x13, RemoteButton.NUM_4 to 0x14, RemoteButton.NUM_5 to 0x15,
                RemoteButton.NUM_6 to 0x16, RemoteButton.NUM_7 to 0x17, RemoteButton.NUM_8 to 0x18,
                RemoteButton.NUM_9 to 0x19
            )
        ),
        BrandIrCodeSet(
            brandKey = "arcelik", displayNameEn = "Arçelik", displayNameLocal = "Arçelik",
            protocol = IrProtocol.RC5, verified = false, address = 0x0,
            commands = mapOf(
                RemoteButton.BLUE to 0x33, RemoteButton.GREEN to 0x2C, RemoteButton.MENU to 0x29,
                RemoteButton.MUTE to 0x2B, RemoteButton.OK to 0x28, RemoteButton.POWER to 0x20,
                RemoteButton.RED to 0x26, RemoteButton.VOLUME_UP to 0x24, RemoteButton.YELLOW to 0x2E,
                RemoteButton.NUM_0 to 0x10, RemoteButton.NUM_1 to 0x11, RemoteButton.NUM_2 to 0x12,
                RemoteButton.NUM_3 to 0x13, RemoteButton.NUM_4 to 0x14, RemoteButton.NUM_5 to 0x15,
                RemoteButton.NUM_6 to 0x16, RemoteButton.NUM_7 to 0x17, RemoteButton.NUM_8 to 0x18,
                RemoteButton.NUM_9 to 0x19
            )
        )
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
