package com.atakolstudio.sure.data.ir

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [AcCodeLibrary], uygulamaya gömülü `assets/generic_ac_codes.json` dosyasını okur.
 * Bu testler, dosyanın gerçekten var olduğunu, doğru ayrıştırıldığını ve beklenen
 * durumlar için geçerli darbe dizileri döndürdüğünü doğrular. Gerçek Android
 * `assets` erişimi gerektirdiğinden Robolectric kullanılır.
 */
@RunWith(RobolectricTestRunner::class)
class AcCodeLibraryTest {

    private lateinit var library: AcCodeLibrary

    @Before
    fun setUp() {
        library = AcCodeLibrary(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `assets dosyasi basariyla yukleniyor`() {
        assertThat(library.hasData()).isTrue()
    }

    @Test
    fun `desteklenen sicaklik araligi 17-25 arasi`() {
        assertThat(library.supportedTemperatureRange).isEqualTo(17..25)
    }

    @Test
    fun `sogutma modu icin gecerli bir kod donuyor`() {
        val code = library.codeForState(AcMode.COOL, 22, AcFanSpeed.MED)
        assertThat(code).isNotNull()
        assertThat(code!!.frequencyHz).isEqualTo(38000)
        assertThat(code.pulses).isNotEmpty()
    }

    @Test
    fun `isitma modu icin gecerli bir kod donuyor`() {
        val code = library.codeForState(AcMode.HEAT, 20, AcFanSpeed.HIGH)
        assertThat(code).isNotNull()
    }

    @Test
    fun `sadece fan modu icin gecerli bir kod donuyor`() {
        val code = library.codeForState(AcMode.FAN, temperature = 22, fanSpeed = AcFanSpeed.LOW)
        assertThat(code).isNotNull()
    }

    @Test
    fun `kapatma kodu mevcut`() {
        assertThat(library.codeForOff()).isNotNull()
    }

    @Test
    fun `AC modu icin sicaklik degeri gormezden gelinir ama kod yine de doner`() {
        val code = library.codeForState(AcMode.OFF, temperature = 99, fanSpeed = AcFanSpeed.LOW)
        assertThat(code).isEqualTo(library.codeForOff())
    }

    @Test
    fun `araligin disindaki sicaklik degeri en yakin gecerli degere sabitlenir`() {
        // 30 derece, desteklenen araligin (17-25) disinda -> 25'e sabitlenmeli
        val outOfRange = library.codeForState(AcMode.COOL, 30, AcFanSpeed.MED)
        val clamped = library.codeForState(AcMode.COOL, 25, AcFanSpeed.MED)
        assertThat(outOfRange).isEqualTo(clamped)
    }

    @Test
    fun `farkli fan hizlari farkli kodlar uretir`() {
        val low = library.codeForState(AcMode.COOL, 22, AcFanSpeed.LOW)
        val high = library.codeForState(AcMode.COOL, 22, AcFanSpeed.HIGH)
        assertThat(low).isNotNull()
        assertThat(high).isNotNull()
        assertThat(low!!.pulses).isNotEqualTo(high!!.pulses)
    }
}
