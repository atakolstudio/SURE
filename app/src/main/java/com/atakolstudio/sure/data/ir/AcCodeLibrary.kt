package com.atakolstudio.sure.data.ir

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

enum class AcMode { COOL, HEAT, FAN, OFF }
enum class AcFanSpeed { LOW, MED, HIGH }

/**
 * Klima uzaktan kumandalarında -TV'lerin aksine- her tuş basımı o anki TÜM durumu
 * (sıcaklık + mod + fan hızı) tek bir uzun sinyalde kodlar ("tam durum" protokolü).
 * Bu yüzden basit bir protokol/adres/komut modeliyle ifade edilemezler; gerçek bir
 * kumandadan (LIRC açık kaynak veritabanı, "hokkaido" jenerik klima modülü) yakalanmış
 * tam darbe dizileri olarak saklanır ve durum değiştiğinde ilgili tam kayıt yeniden
 * gönderilir.
 *
 * NOT: Bu, TEK bir jenerik/örnek klima profilidir (birçok ucuz/OEM klimada kullanılan
 * yaygın bir modül). Daikin, Mitsubishi, Gree gibi büyük markaların kendi özel
 * algoritmaları farklıdır ve ayrı olarak eklenmesi gerekir.
 */
@Singleton
class AcCodeLibrary @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class AcPulseCode(val frequencyHz: Int, val pulses: IntArray)

    private var cache: Map<String, AcPulseCode>? = null

    private fun load(): Map<String, AcPulseCode> {
        cache?.let { return it }
        val result = runCatching {
            val text = context.assets.open(ASSET_FILE_NAME).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val json = JSONObject(text)
            val map = LinkedHashMap<String, AcPulseCode>()
            json.keys().forEach { key ->
                val entry = json.getJSONObject(key)
                val freq = entry.getInt("freq")
                val pulsesArray = entry.getJSONArray("pulses")
                val pulses = IntArray(pulsesArray.length()) { i -> pulsesArray.getInt(i) }
                map[key] = AcPulseCode(freq, pulses)
            }
            map
        }.getOrElse { emptyMap() }
        cache = result
        return result
    }

    /** Bu jenerik profilde desteklenen sıcaklık aralığı (°C). */
    val supportedTemperatureRange: IntRange = 17..25

    fun hasData(): Boolean = load().isNotEmpty()

    fun codeForOff(): AcPulseCode? = load()["OFF"]

    fun codeForFanOnly(speed: AcFanSpeed): AcPulseCode? = load()["FAN_${speed.name}"]

    fun codeForState(mode: AcMode, temperature: Int, fanSpeed: AcFanSpeed): AcPulseCode? {
        if (mode == AcMode.OFF) return codeForOff()
        if (mode == AcMode.FAN) return codeForFanOnly(fanSpeed)
        val clampedTemp = temperature.coerceIn(supportedTemperatureRange)
        return load()["${mode.name}_${clampedTemp}_${fanSpeed.name}"]
    }

    companion object {
        private const val ASSET_FILE_NAME = "generic_ac_codes.json"
    }
}
