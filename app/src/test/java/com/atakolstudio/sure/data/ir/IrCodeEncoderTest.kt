package com.atakolstudio.sure.data.ir

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [IrCodeEncoder] uygulamanın en kritik bileşenidir: burada üretilen darbe dizisi
 * yanlış olursa, kullanıcı yanlış cihaza yanlış sinyal gönderir (ya da hiçbir şey
 * olmaz). Bu testler, her protokolün üretim kurallarına (header zamanlaması, bit
 * sayısı, LSB/MSB sırası) uyduğunu doğrular.
 */
class IrCodeEncoderTest {

    // ------------------------------------------------------------------
    // NEC
    // ------------------------------------------------------------------

    @Test
    fun `NEC - header zamanlamasi dogru`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.NEC, address = 0x04, command = 0x08))
        assertThat(pattern[0]).isEqualTo(9000)
        assertThat(pattern[1]).isEqualTo(4500)
    }

    @Test
    fun `NEC - toplam darbe sayisi dogru (header + 32 bit + kapanis)`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.NEC, address = 0x04, command = 0x08))
        // 2 (header) + 32 bit * 2 (mark+space) + 1 (kapanis mark) = 67
        assertThat(pattern.size).isEqualTo(67)
    }

    @Test
    fun `NEC - tum darbe degerleri pozitif`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.NEC, address = 0x04, command = 0x08))
        assertThat(pattern.all { it > 0 }).isTrue()
    }

    @Test
    fun `NEC - genisletilmis adres verilmezse tumleç adres kullanilir`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.NEC, address = 0x04, command = 0x08))
        val bits = decodeNecBits(pattern)
        val addr = bitsToByte(bits, 0)
        val addrInv = bitsToByte(bits, 8)
        assertThat(addrInv).isEqualTo(addr.inv() and 0xFF)
    }

    @Test
    fun `NEC - komut ve tumleci dogru kodlanir`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.NEC, address = 0x04, command = 0x08))
        val bits = decodeNecBits(pattern)
        val command = bitsToByte(bits, 16)
        val commandInv = bitsToByte(bits, 24)
        assertThat(command).isEqualTo(0x08)
        assertThat(commandInv).isEqualTo(0x08.inv() and 0xFF)
    }

    @Test
    fun `NEC - genisletilmis adres verilirse dogrudan kullanilir (tumlec degil)`() {
        val pattern = IrCodeEncoder.encode(
            IrCommand(IrProtocol.NEC, address = 0x04, command = 0x08, extendedAddress = 0x99)
        )
        val bits = decodeNecBits(pattern)
        val addrInv = bitsToByte(bits, 8)
        assertThat(addrInv).isEqualTo(0x99)
    }

    // ------------------------------------------------------------------
    // JVC
    // ------------------------------------------------------------------

    @Test
    fun `JVC - header zamanlamasi dogru`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.JVC, address = 0x03, command = 0x17))
        assertThat(pattern[0]).isEqualTo(8000)
        assertThat(pattern[1]).isEqualTo(4000)
    }

    @Test
    fun `JVC - toplam darbe sayisi dogru (header + 16 bit + kapanis, tumlec yok)`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.JVC, address = 0x03, command = 0x17))
        // 2 (header) + 16 bit * 2 + 1 (kapanis) = 35
        assertThat(pattern.size).isEqualTo(35)
    }

    // ------------------------------------------------------------------
    // Samsung
    // ------------------------------------------------------------------

    @Test
    fun `Samsung - header zamanlamasi dogru (4500-4500)`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.SAMSUNG, address = 0x07, command = 0x02))
        assertThat(pattern[0]).isEqualTo(4500)
        assertThat(pattern[1]).isEqualTo(4500)
    }

    @Test
    fun `Samsung - toplam darbe sayisi NEC ile ayni yapida (32 bit)`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.SAMSUNG, address = 0x07, command = 0x02))
        assertThat(pattern.size).isEqualTo(67)
    }

    // ------------------------------------------------------------------
    // Sony SIRC (12/15/20 bit)
    // ------------------------------------------------------------------

    @Test
    fun `Sony SIRC12 - header zamanlamasi dogru`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.SONY_SIRC12, address = 0x01, command = 0x15))
        assertThat(pattern[0]).isEqualTo(2400)
        assertThat(pattern[1]).isEqualTo(600)
    }

    @Test
    fun `Sony SIRC12 - 12 bit icin dogru darbe sayisi`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.SONY_SIRC12, address = 0x01, command = 0x15))
        // 2 (header) + 11 bit * 2 (ara) + 1 bit * 1 (son bit, kapanis space'i yok) = 2+22+1=25
        assertThat(pattern.size).isEqualTo(25)
    }

    @Test
    fun `Sony SIRC15 - 15 bit icin daha uzun darbe dizisi uretir`() {
        val pattern12 = IrCodeEncoder.encode(IrCommand(IrProtocol.SONY_SIRC12, address = 0x01, command = 0x15))
        val pattern15 = IrCodeEncoder.encode(IrCommand(IrProtocol.SONY_SIRC15, address = 0x01, command = 0x15))
        assertThat(pattern15.size).isGreaterThan(pattern12.size)
    }

    // ------------------------------------------------------------------
    // RC5 / RC6
    // ------------------------------------------------------------------

    @Test
    fun `RC5 - Manchester kodlamasinda her bit 2 darbe uretir`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.RC5, address = 0x00, command = 0x0C))
        // 14 bit (2 start + 1 toggle + 5 adres + 6 komut) * 2 (Manchester: her bit icin mark+space) = 28
        assertThat(pattern.size).isEqualTo(28)
    }

    @Test
    fun `RC6 - leader ve start bit yapisi dogru uzunlukta`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.RC6, address = 0x00, command = 0x0C))
        assertThat(pattern[0]).isEqualTo(2666) // leader mark
        assertThat(pattern[1]).isEqualTo(889)  // leader space
        assertThat(pattern.all { it > 0 }).isTrue()
    }

    // ------------------------------------------------------------------
    // Panasonic
    // ------------------------------------------------------------------

    @Test
    fun `Panasonic - header zamanlamasi dogru`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.PANASONIC, address = 0x40, command = 0x3D, extendedAddress = 0x04))
        assertThat(pattern[0]).isEqualTo(3502)
        assertThat(pattern[1]).isEqualTo(1750)
    }

    @Test
    fun `Panasonic - 48 bit veri icin dogru darbe sayisi`() {
        val pattern = IrCodeEncoder.encode(IrCommand(IrProtocol.PANASONIC, address = 0x40, command = 0x3D, extendedAddress = 0x04))
        // 2 (header) + 48 bit * 2 + 1 (kapanis) = 99
        assertThat(pattern.size).isEqualTo(99)
    }

    // ------------------------------------------------------------------
    // Genel: tüm protokoller için farklı komutlar farklı sonuç üretmeli
    // ------------------------------------------------------------------

    @Test
    fun `farkli komutlar farkli darbe dizileri uretir`() {
        val power = IrCodeEncoder.encode(IrCommand(IrProtocol.NEC, address = 0x04, command = 0x08))
        val volumeUp = IrCodeEncoder.encode(IrCommand(IrProtocol.NEC, address = 0x04, command = 0x02))
        assertThat(power).isNotEqualTo(volumeUp)
    }

    @Test
    fun `farkli adresler farkli darbe dizileri uretir`() {
        val brand1 = IrCodeEncoder.encode(IrCommand(IrProtocol.NEC, address = 0x04, command = 0x08))
        val brand2 = IrCodeEncoder.encode(IrCommand(IrProtocol.NEC, address = 0x40, command = 0x08))
        assertThat(brand1).isNotEqualTo(brand2)
    }

    // ------------------------------------------------------------------
    // Yardımcı fonksiyonlar: NEC darbe dizisini geri bit dizisine çevirir
    // (encoder'ın gerçekten doğru bitleri gönderdiğini doğrulamak için)
    // ------------------------------------------------------------------

    private fun decodeNecBits(pattern: IntArray): List<Int> {
        val bits = mutableListOf<Int>()
        var i = 2
        repeat(32) {
            val space = pattern[i + 1]
            bits += if (space > 1000) 1 else 0
            i += 2
        }
        return bits
    }

    private fun bitsToByte(bits: List<Int>, startIndex: Int): Int {
        var value = 0
        for (i in 0 until 8) {
            value = value or (bits[startIndex + i] shl i) // LSB-first
        }
        return value
    }
}
