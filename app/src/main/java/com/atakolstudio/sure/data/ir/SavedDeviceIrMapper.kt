package com.atakolstudio.sure.data.ir

import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity
import org.json.JSONObject

/** Ozel/manuel bulunan cihazlar icin ayirt edici marka anahtari. */
const val CUSTOM_BRAND_KEY = "custom"

/**
 * Kayitli bir cihazi, IR gonderimi icin kullanilan [BrandIrCodeSet] modeline cevirir.
 * - Bilinen bir marka ise: BrandIrDatabase icinden bulunur.
 * - Ozel/manuel bulunan bir cihazsa (brandKey == "custom"): custom* alanlarindan
 *   yeniden olusturulur.
 */
fun SavedDeviceEntity.resolveBrandIrCodeSet(): BrandIrCodeSet? {
    if (brandKey != CUSTOM_BRAND_KEY) {
        return BrandIrDatabase.findByKey(brandKey)
    }

    val protocolName = customProtocol ?: return null
    val address = customAddress ?: return null
    val protocol = runCatching { IrProtocol.valueOf(protocolName) }.getOrNull() ?: return null
    val commandsJson = customCommandsJson ?: return null

    val commands = mutableMapOf<RemoteButton, Int>()
    runCatching {
        val json = JSONObject(commandsJson)
        json.keys().forEach { key ->
            val button = runCatching { RemoteButton.valueOf(key) }.getOrNull()
            if (button != null) {
                commands[button] = json.getInt(key)
            }
        }
    }
    if (commands.isEmpty()) return null

    return BrandIrCodeSet(
        brandKey = CUSTOM_BRAND_KEY,
        displayNameEn = brandDisplayName,
        displayNameLocal = brandDisplayName,
        protocol = protocol,
        verified = true, // kullanici bizzat test edip onayladigi icin dogrulanmis sayilir
        address = address,
        extendedAddress = customExtendedAddress,
        commands = commands
    )
}

/** Bir komut haritasini (RemoteButton -> kod) JSON string'e cevirir (Room'da saklamak icin). */
fun Map<RemoteButton, Int>.toJsonString(): String {
    val json = JSONObject()
    forEach { (button, code) -> json.put(button.name, code) }
    return json.toString()
}

/**
 * NEC ailesi protokoller icin, kullanicinin buldugu tek bir adres ile tam bir tus
 * haritasi olusturur (genericNecCommands sablonu + kullanicinin test ettigi POWER kodu).
 * Bu, "buldum ama sadece guc tusunu test ettim" durumunda diger tuslarin da (buyuk
 * ihtimalle) calismasini saglayan pratik bir varsayimdir.
 */
fun buildNecTemplateCommands(testedPowerCommand: Int): Map<RemoteButton, Int> {
    val template = BrandIrDatabase.brands.first { it.brandKey == "toshiba" }.commands.toMutableMap()
    template[RemoteButton.POWER] = testedPowerCommand
    return template
}
