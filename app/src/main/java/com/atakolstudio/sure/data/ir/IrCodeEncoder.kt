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
    // Philips RC5 — Manchester (bi-phase) kodlama, 889us birim.
    // 14 bit: start(1) + field/start2(1) + toggle(1) + adres(5) + komut(6, alt bitler).
    // "Genişletilmiş RC5" kuralına göre field/start2 biti, 7-bitlik komutun en
    // anlamlı bitinin (MSB) TERSİ olarak kodlanır — böylece 64-127 aralığındaki
    // komutlar da (birçok gerçek TV kumandasında kullanılır) doğru desteklenir.
    //
    // Manchester'da bit=1 → yarı periyotta KAPALI'dan AÇIK'a geçiş (space,mark);
    // bit=0 → AÇIK'tan KAPALI'ya geçiş (mark,space). Ardışık iki bit sınırında
    // aynı türden (ikisi de "açık" ya da ikisi de "kapalı") segmentler varsa,
    // bunlar TEK bir daha uzun darbede birleştirilmelidir — aksi halde
    // ConsumerIrManager'ın beklediği "sürekli alternatif açık/kapalı" dizisi bozulur.
    // ---------------------------------------------------------------------
    private fun encodeRc5(cmd: IrCommand): IntArray {
        val unit = 889

        val commandMsb = (cmd.command shr 6) and 1
        val fieldBit = commandMsb xor 1 // genişletilmiş komut MSB'sinin tersi

        val bits = mutableListOf<Int>()
        bits += 1        // start bit 1 (sabit)
        bits += fieldBit // start bit 2 / field bit
        bits += 0        // toggle bit
        appendBitsMsbFirst(bits, cmd.address, 5)
        appendBitsMsbFirst(bits, cmd.command, 6) // komutun alt 6 biti

        // Her bit icin (isMark, sure) segment cifti uret. bit=1 -> mark,space
        // (start biti HER ZAMAN 1'dir, bu yuzden dizi mutlaka mark ile baslamalidir —
        // ConsumerIrManager pattern[0]'in acik/on suresi olmasini sart kosar).
        // bit=0 -> space,mark.
        data class Segment(val isMark: Boolean, val duration: Int)
        val segments = mutableListOf<Segment>()
        for (bit in bits) {
            if (bit == 1) {
                segments += Segment(isMark = true, duration = unit)  // mark
                segments += Segment(isMark = false, duration = unit) // space
            } else {
                segments += Segment(isMark = false, duration = unit) // space
                segments += Segment(isMark = true, duration = unit)  // mark
            }
        }

        // Ardışık aynı-turdeki segmentleri birlestir
        val merged = mutableListOf<Segment>()
        for (seg in segments) {
            val last = merged.lastOrNull()
            if (last != null && last.isMark == seg.isMark) {
                merged[merged.lastIndex] = Segment(last.isMark, last.duration + seg.duration)
            } else {
                merged += seg
            }
        }

        // RC5'in ilk biti (start bit 1) her zaman 1'dir, bu yüzden dizi her zaman
        // "mark" ile başlar — ConsumerIrManager'ın beklediği format budur.
        return merged.map { it.duration }.toIntArray()
    }

    // ---------------------------------------------------------------------
    // RC6 — 2666us leader + 889us space + start bit(1) + 3 mode bit + toggle(2x genişlik)
    // + 8bit adres + 8bit komut. RC5'teki gibi, ardışık aynı-türden (mark/space)
    // segmentler birleştirilir (bkz. encodeRc5 açıklaması).
    // ---------------------------------------------------------------------
    private fun encodeRc6(cmd: IrCommand): IntArray {
        val unit = 444

        data class Segment(val isMark: Boolean, val duration: Int)
        val segments = mutableListOf<Segment>()

        segments += Segment(isMark = true, duration = 2666)  // leader mark
        segments += Segment(isMark = false, duration = 889)  // leader space

        // Start biti her zaman 1 (normal genişlik): mark,space
        segments += Segment(isMark = true, duration = unit)
        segments += Segment(isMark = false, duration = unit)

        // Mode bitleri 000 (basitleştirilmiş, mode 0): her biri bit=0 -> space,mark
        repeat(3) {
            segments += Segment(isMark = false, duration = unit)
            segments += Segment(isMark = true, duration = unit)
        }

        // Toggle biti 0, ÇİFT genişlikte: bit=0 -> space,mark (2x unit)
        segments += Segment(isMark = false, duration = unit * 2)
        segments += Segment(isMark = true, duration = unit * 2)

        val bits = mutableListOf<Int>()
        appendBitsMsbFirst(bits, cmd.address, 8)
        appendBitsMsbFirst(bits, cmd.command, 8)
        for (bit in bits) {
            if (bit == 1) {
                segments += Segment(isMark = true, duration = unit)
                segments += Segment(isMark = false, duration = unit)
            } else {
                segments += Segment(isMark = false, duration = unit)
                segments += Segment(isMark = true, duration = unit)
            }
        }

        val merged = mutableListOf<Segment>()
        for (seg in segments) {
            val last = merged.lastOrNull()
            if (last != null && last.isMark == seg.isMark) {
                merged[merged.lastIndex] = Segment(last.isMark, last.duration + seg.duration)
            } else {
                merged += seg
            }
        }
        return merged.map { it.duration }.toIntArray()
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
