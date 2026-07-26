package com.atakolstudio.sure.data.ir

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [BrandIrDatabase] için veri bütünlüğü testleri. Bunlar "IR kodu doğru mu"
 * sorusunu değil, "veritabanı yapısal olarak tutarlı mı" sorusunu test eder
 * (yinelenen anahtar yok, her marka Güç tuşuna sahip, adresler protokolün bit
 * sınırları içinde vb.) — insan hatasıyla eklenen bozuk bir marka girdisini
 * derleme zamanında değil ama test zamanında yakalar.
 */
class BrandIrDatabaseTest {

    @Test
    fun `hicbir marka anahtari tekrar etmiyor`() {
        val keys = BrandIrDatabase.brands.map { it.brandKey }
        assertThat(keys).containsNoDuplicates()
    }

    @Test
    fun `her marka en azindan GUC tusuna sahip`() {
        val brandsWithoutPower = BrandIrDatabase.brands.filter { RemoteButton.POWER !in it.commands }
        assertThat(brandsWithoutPower).isEmpty()
    }

    @Test
    fun `her markanin en az bir komutu var`() {
        val emptyBrands = BrandIrDatabase.brands.filter { it.commands.isEmpty() }
        assertThat(emptyBrands).isEmpty()
    }

    @Test
    fun `RC5 protokolundeki markalarin adresi 5 bit sinirinda (0-31)`() {
        val rc5Brands = BrandIrDatabase.brands.filter { it.protocol == IrProtocol.RC5 }
        assertThat(rc5Brands).isNotEmpty()
        rc5Brands.forEach { brand ->
            assertThat(brand.address).isIn(0..31)
        }
    }

    @Test
    fun `RC5 protokolundeki markalarin tum komutlari 6 bit sinirinda (0-63)`() {
        val rc5Brands = BrandIrDatabase.brands.filter { it.protocol == IrProtocol.RC5 }
        rc5Brands.forEach { brand ->
            brand.commands.values.forEach { command ->
                assertThat(command).isIn(0..63)
            }
        }
    }

    @Test
    fun `Sony SIRC12 markalarin adresi 5 bit sinirinda`() {
        val sonyBrands = BrandIrDatabase.brands.filter { it.protocol == IrProtocol.SONY_SIRC12 }
        sonyBrands.forEach { brand ->
            assertThat(brand.address).isIn(0..31)
        }
    }

    @Test
    fun `NEC ve Samsung markalarin adres ve komutlari byte sinirinda (0-255)`() {
        val necLikeBrands = BrandIrDatabase.brands.filter {
            it.protocol == IrProtocol.NEC || it.protocol == IrProtocol.SAMSUNG || it.protocol == IrProtocol.JVC
        }
        necLikeBrands.forEach { brand ->
            assertThat(brand.address).isIn(0..255)
            brand.commands.values.forEach { command ->
                assertThat(command).isIn(0..255)
            }
        }
    }

    @Test
    fun `findByKey bilinen bir marka icin dogru sonucu dondurur`() {
        val samsung = BrandIrDatabase.findByKey("samsung")
        assertThat(samsung).isNotNull()
        assertThat(samsung!!.displayNameEn).isEqualTo("Samsung")
    }

    @Test
    fun `findByKey bilinmeyen bir anahtar icin null dondurur`() {
        assertThat(BrandIrDatabase.findByKey("bu_marka_yok_12345")).isNull()
    }

    @Test
    fun `toIrCommand marka bu tusu desteklemiyorsa null dondurur`() {
        // Panasonic veritabaninda renkli tuslar tanimli degil
        val panasonic = BrandIrDatabase.findByKey("panasonic")!!
        val result = BrandIrDatabase.toIrCommand(panasonic, RemoteButton.RED)
        assertThat(result).isNull()
    }

    @Test
    fun `toIrCommand marka tusu destekliyorsa dogru IrCommand uretir`() {
        val samsung = BrandIrDatabase.findByKey("samsung")!!
        val result = BrandIrDatabase.toIrCommand(samsung, RemoteButton.POWER)
        assertThat(result).isNotNull()
        assertThat(result!!.protocol).isEqualTo(IrProtocol.SAMSUNG)
        assertThat(result.address).isEqualTo(samsung.address)
        assertThat(result.command).isEqualTo(0x02)
    }

    @Test
    fun `GENERIC_AC_PLACEHOLDER normal marka listesinde yer almiyor`() {
        val keys = BrandIrDatabase.brands.map { it.brandKey }
        assertThat(keys).doesNotContain("generic_ac")
    }

    @Test
    fun `dogrulanmamis (verified=false) markalar en azindan genel bir tus setine sahip`() {
        val unverified = BrandIrDatabase.brands.filter { !it.verified }
        unverified.forEach { brand ->
            assertThat(brand.commands.size).isAtLeast(5)
        }
    }
}
