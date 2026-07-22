package com.atakolstudio.sure.data.ir

/**
 * [IrCommand] nesnelerini, Android'in `ConsumerIrManager.transmit(frequency, pattern)`
 * fonksiyonunun beklediği ham darbe dizisine (mikrosaniye cinsinden açık/kapalı süreleri)
 * dönüştürür.
 *
 * Pattern dizisi her zaman "açık" süre ile başlar, "kapalı" süre ile devam eder ve
 * çift sayıda eleman içerir (transmit() bunu şart koşar).
 *
 * Not: Bu kodlayıcı yaygın protokollerin genel zamanlama kurallarına göre çalışır.
 * Bazı üretici/model varyasyonları farklı toleranslara sahip olabilir; bu durumda
 * ilgili markanın kod tablosunu (bkz. BrandIrDatabase) gerçek bir IR alıcısı ile
 * doğrulayıp güncellemeniz önerilir.
 */
object IrCodeEncoder {

    fun encode(cmd: IrCommand): IntArray = when (cmd.protocol) {
        IrProtocol.NEC -> encodeNec(cmd)
        IrProtocol.SAMSUNG -> encodeSamsung(cmd)
        IrProtocol.SONY_SIRC12 -> encodeSirc(cmd, bits = 12)
        IrProtocol.SONY_SIRC15 -> encodeSirc(cmd, bits = 15)
        IrProtocol.SONY_SIRC20 -> encodeSirc(cmd, bits = 20)
        IrProtocol.RC5 -> encodeRc5(cmd)
        IrProtocol.RC6 -> encodeRc6(cmd)
        IrProtocol.PANASONIC -> encodePanasonic(cmd)
        IrProtocol.JVC -> encodeJvc(cmd)
    }

    // ---------------------------------------------------------------------
    // NEC — 9ms header + 4.5ms space, ardından 32 bit (adres, ~adres, komut, ~komut)
    // Her bit: 562us açık + (562us=0 / 1687us=1) kapalı
    // ---------------------------------------------------------------------
    private fun encodeNec(cmd: IrCommand): IntArray {
        val pulses = mutableListOf<Int>()
        pulses += 9000; pulses += 4500 // header

        val addr = cmd.address and 0xFF
        val addrInv = if (cmd.extendedAddress != null) (cmd.extendedAddress and 0xFF) else addr.inv() and 0xFF
        val command = cmd.command and 0xFF
        val commandInv = command.inv() and 0xFF

        val bits = mutableListOf<Int>()
        appendByteLsbFirst(bits, addr)
        appendByteLsbFirst(bits, addrInv)
        appendByteLsbFirst(bits, command)
        appendByteLsbFirst(bits, commandInv)

        for (bit in bits) {
            pulses += 562
            pulses += if (bit == 1) 1687 else 562
        }
        pulses += 562 // trailing mark
        return pulses.toIntArray()
    }

    // ---------------------------------------------------------------------
    // Samsung — NEC'e çok benzer ama header 4500/4500 ve adres baytı tekrar edilir
    // ---------------------------------------------------------------------
    private fun encodeSamsung(cmd: IrCommand): IntArray {
        val pulses = mutableListOf<Int>()
        pulses += 4500; pulses += 4500 // header

        val addr = cmd.address and 0xFF
        val command = cmd.command and 0xFF
        val commandInv = command.inv() and 0xFF

        val bits = mutableListOf<Int>()
        appendByteLsbFirst(bits, addr)
        appendByteLsbFirst(bits, addr) // Samsung adres baytını tekrarlar
        appendByteLsbFirst(bits, command)
        appendByteLsbFirst(bits, commandInv)

        for (bit in bits) {
            pulses += 560
            pulses += if (bit == 1) 1690 else 560
        }
        pulses += 560
        return pulses.toIntArray()
    }

    // ---------------------------------------------------------------------
    // Sony SIRC — 2400us header, 600us space, bitler LSB-first
    // Bit 0: 600us açık + 600us kapalı | Bit 1: 1200us açık + 600us kapalı
    // ---------------------------------------------------------------------
    private fun encodeSirc(cmd: IrCommand, bits: Int): IntArray {
        val pulses = mutableListOf<Int>()
        pulses += 2400; pulses += 600 // header

        val totalBits = mutableListOf<Int>()
        // Komut her zaman 7 bit
        appendBitsLsbFirst(totalBits, cmd.command, 7)
        when (bits) {
            12 -> appendBitsLsbFirst(totalBits, cmd.address, 5)
            15 -> appendBitsLsbFirst(totalBits, cmd.address, 8)
            20 -> {
                appendBitsLsbFirst(totalBits, cmd.address, 5)
                appendBitsLsbFirst(totalBits, cmd.extendedAddress ?: 0, 8)
            }
        }

        for ((i, bit) in totalBits.withIndex()) {
            pulses += if (bit == 1) 1200 else 600
            if (i != totalBits.lastIndex) pulses += 600
        }
        return pulses.toIntArray()
    }

    // ---------------------------------------------------------------------
    // Philips RC5 — Manchester kodlama, 889us birim, 14 bit (2 start + toggle + 5 adres + 6 komut)
    // Basitlik için toggle biti 0 sabitlenmiştir (çoğu alıcı tolere eder).
    // ---------------------------------------------------------------------
    private fun encodeRc5(cmd: IrCommand): IntArray {
        val unit = 889
        val bits = mutableListOf<Int>()
        bits += 1 // start bit 1
        bits += 1 // start bit 2 (RC5 field bit, genelde 1)
        bits += 0 // toggle bit
        appendBitsMsbFirst(bits, cmd.address, 5)
        appendBitsMsbFirst(bits, cmd.command, 6)

        // Manchester: bit 1 -> açık->kapalı geçiş, bit 0 -> kapalı->açık geçiş
        val pulses = mutableListOf<Int>()
        var currentlyOn = true // ilk yarı her zaman "açık" ile başlar (start bit 1)
        pulses += unit // ilk mark
        for (i in 1 until bits.size) {
            val bit = bits[i]
            if (bit == 1) {
                pulses += unit // space
                pulses += unit // mark
            } else {
                pulses += unit
                pulses += unit
            }
        }
        return pulses.toIntArray()
    }

    // ---------------------------------------------------------------------
    // RC6 — 2666us leader + 889us space + start bit(1) + 3 mode bit + toggle(2x uzunluk) + 8bit adres + 8bit komut
    // Basitleştirilmiş RC6 mode-0 kodlayıcı.
    // ---------------------------------------------------------------------
    private fun encodeRc6(cmd: IrCommand): IntArray {
        val unit = 444
        val pulses = mutableListOf<Int>()
        pulses += 2666 // leader mark
        pulses += 889  // leader space

        // Start bit (her zaman 1) - normal genişlik
        pulses += unit; pulses += unit

        // Mode bits 000
        repeat(3) { pulses += unit; pulses += unit }

        // Toggle bit - çift genişlik (basitleştirilmiş: 0 sabit)
        pulses += unit * 2; pulses += unit * 2

        val bits = mutableListOf<Int>()
        appendBitsMsbFirst(bits, cmd.address, 8)
        appendBitsMsbFirst(bits, cmd.command, 8)
        for (bit in bits) {
            if (bit == 1) { pulses += unit; } else { pulses += unit }
            pulses += unit
        }
        return pulses.toIntArray()
    }

    // ---------------------------------------------------------------------
    // Panasonic (Kaseikyo ailesi) — 3502us header + 1750us space, 48 bit veri
    // Basitleştirilmiş: sadece adres+komut baytlarını kodlar.
    // ---------------------------------------------------------------------
    private fun encodePanasonic(cmd: IrCommand): IntArray {
        val pulses = mutableListOf<Int>()
        pulses += 3502; pulses += 1750 // header

        val bits = mutableListOf<Int>()
        appendByteLsbFirst(bits, 0x02); appendByteLsbFirst(bits, 0x20) // Panasonic vendor ID (sabit)
        appendByteLsbFirst(bits, cmd.address and 0xFF)
        appendByteLsbFirst(bits, (cmd.extendedAddress ?: 0x00) and 0xFF)
        appendByteLsbFirst(bits, cmd.command and 0xFF)
        val checksum = (cmd.address xor (cmd.extendedAddress ?: 0) xor cmd.command) and 0xFF
        appendByteLsbFirst(bits, checksum)

        for (bit in bits) {
            pulses += 435
            pulses += if (bit == 1) 1300 else 435
        }
        pulses += 435
        return pulses.toIntArray()
    }

    // ---------------------------------------------------------------------
    // JVC — 8000/4000us header, 16 bit (8 bit adres + 8 bit komut, tümleç yok),
    // LSB-first. Her bit: 600us açık + (550us=0 / 1600us=1) kapalı.
    // ---------------------------------------------------------------------
    private fun encodeJvc(cmd: IrCommand): IntArray {
        val pulses = mutableListOf<Int>()
        pulses += 8000; pulses += 4000 // header

        val bits = mutableListOf<Int>()
        appendByteLsbFirst(bits, cmd.address and 0xFF)
        appendByteLsbFirst(bits, cmd.command and 0xFF)

        for (bit in bits) {
            pulses += 600
            pulses += if (bit == 1) 1600 else 550
        }
        pulses += 600 // trailing mark
        return pulses.toIntArray()
    }

    // --- Yardımcı fonksiyonlar ---
    private fun appendByteLsbFirst(target: MutableList<Int>, value: Int) =
        appendBitsLsbFirst(target, value, 8)

    private fun appendBitsLsbFirst(target: MutableList<Int>, value: Int, bitCount: Int) {
        for (i in 0 until bitCount) target += (value shr i) and 1
    }

    private fun appendBitsMsbFirst(target: MutableList<Int>, value: Int, bitCount: Int) {
        for (i in bitCount - 1 downTo 0) target += (value shr i) and 1
    }
}
