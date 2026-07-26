package com.atakolstudio.sure.data.ir

import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * [SavedDeviceIrMapper] JSON serileştirme/geri-okuma mantığını test eder. `org.json`
 * gerçek Android çalışma zamanında çalıştığından, JVM birim testlerinde gerçekçi
 * davranış almak için Robolectric kullanılır (aksi halde org.json çağrıları sahte/boş
 * değerler döndürür).
 */
@RunWith(RobolectricTestRunner::class)
class SavedDeviceIrMapperTest {

    @Test
    fun `komut haritasi JSON'a cevrilip geri okunabiliyor (round-trip)`() {
        val original = mapOf(
            RemoteButton.POWER to 0x12,
            RemoteButton.VOLUME_UP to 0x1A,
            RemoteButton.CHANNEL_DOWN to 0x01
        )

        val json = original.toJsonString()
        assertThat(json).contains("POWER")
        assertThat(json).contains("18") // 0x12 = 18 (decimal olarak saklanir)

        // Geri okuma, resolveBrandIrCodeSet uzerinden dolayli test edilir
        val entity = customDeviceEntity(customCommandsJson = json)
        val resolved = entity.resolveBrandIrCodeSet()

        assertThat(resolved).isNotNull()
        assertThat(resolved!!.commands).isEqualTo(original)
    }

    @Test
    fun `bilinen bir marka anahtari icin BrandIrDatabase'den cozumlenir`() {
        val entity = SavedDeviceEntity(
            id = 1,
            nickname = "Salon TV",
            brandKey = "samsung",
            brandDisplayName = "Samsung",
            deviceType = "TV",
            connectionType = "TRADITIONAL_IR",
            createdAtEpochMillis = 0L,
            lastUsedEpochMillis = 0L
        )

        val resolved = entity.resolveBrandIrCodeSet()

        assertThat(resolved).isNotNull()
        assertThat(resolved!!.brandKey).isEqualTo("samsung")
        assertThat(resolved.protocol).isEqualTo(IrProtocol.SAMSUNG)
    }

    @Test
    fun `generic_ac marka anahtari GENERIC_AC_PLACEHOLDER'a cozumlenir`() {
        val entity = SavedDeviceEntity(
            id = 1,
            nickname = "Salon Klimasi",
            brandKey = "generic_ac",
            brandDisplayName = "Jenerik Klima",
            deviceType = "AC",
            connectionType = "TRADITIONAL_IR",
            createdAtEpochMillis = 0L,
            lastUsedEpochMillis = 0L
        )

        val resolved = entity.resolveBrandIrCodeSet()

        assertThat(resolved).isEqualTo(BrandIrDatabase.GENERIC_AC_PLACEHOLDER)
    }

    @Test
    fun `custom protokol veya adres eksikse null doner`() {
        val entity = SavedDeviceEntity(
            id = 1,
            nickname = "Bozuk Cihaz",
            brandKey = CUSTOM_BRAND_KEY,
            brandDisplayName = "Bozuk",
            deviceType = "TV",
            connectionType = "TRADITIONAL_IR",
            createdAtEpochMillis = 0L,
            lastUsedEpochMillis = 0L,
            customProtocol = null, // eksik!
            customAddress = 5,
            customCommandsJson = mapOf(RemoteButton.POWER to 1).toJsonString()
        )

        assertThat(entity.resolveBrandIrCodeSet()).isNull()
    }

    @Test
    fun `bilinmeyen bir marka anahtari icin null doner`() {
        val entity = SavedDeviceEntity(
            id = 1,
            nickname = "Yok Boyle Marka",
            brandKey = "bu_marka_asla_olmayacak",
            brandDisplayName = "?",
            deviceType = "TV",
            connectionType = "TRADITIONAL_IR",
            createdAtEpochMillis = 0L,
            lastUsedEpochMillis = 0L
        )

        assertThat(entity.resolveBrandIrCodeSet()).isNull()
    }

    @Test
    fun `buildNecTemplateCommands test edilen POWER kodunu sablona uygular`() {
        val commands = buildNecTemplateCommands(testedPowerCommand = 0x99)
        assertThat(commands[RemoteButton.POWER]).isEqualTo(0x99)
        // Sablonun diger tuslari (ornegin VOLUME_UP) korunmus olmali
        assertThat(commands).containsKey(RemoteButton.VOLUME_UP)
        assertThat(commands.size).isGreaterThan(1)
    }

    private fun customDeviceEntity(customCommandsJson: String): SavedDeviceEntity = SavedDeviceEntity(
        id = 1,
        nickname = "Test Cihaz",
        brandKey = CUSTOM_BRAND_KEY,
        brandDisplayName = "Test Cihaz",
        deviceType = "TV",
        connectionType = "TRADITIONAL_IR",
        createdAtEpochMillis = 0L,
        lastUsedEpochMillis = 0L,
        customProtocol = IrProtocol.NEC.name,
        customAddress = 0x20,
        customExtendedAddress = null,
        customCommandsJson = customCommandsJson
    )
}
