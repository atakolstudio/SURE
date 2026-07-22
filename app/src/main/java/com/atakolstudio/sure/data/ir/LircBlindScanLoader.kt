package com.atakolstudio.sure.data.ir

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uygulama içine gömülü `assets/lirc_blind_scan.json` dosyasını okuyup, "Kör Tarama"
 * özelliği için gerçek, LIRC (Linux Infrared Remote Control) açık kaynak veritabanından
 * türetilmiş cihaz kodu adaylarına dönüştürür.
 *
 * Bu dosya, github.com/probonopd/lirc-remotes aynasındaki ~2650 kumanda konfigürasyon
 * dosyasının tamamı taranıp, desteklediğimiz protokollerde (NEC, RC5, RC6, Sony SIRC,
 * Panasonic, JVC) POWER tuşu içeren ve birbirinden farklı (protokol, adres, komut)
 * üçlüsüne sahip ~371 benzersiz, GERÇEK kumandadan derlenmiştir. Bu sayede "Kör Tarama",
 * rastgele üretilmiş bir kod ızgarası yerine gerçek dünyada kullanılmış kodları dener —
 * tıpkı profesyonel evrensel kumandaların yaptığı gibi.
 */
@Singleton
class LircBlindScanLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cached: List<BrandIrCodeSet>? = null

    fun loadCandidates(): List<BrandIrCodeSet> {
        cached?.let { return it }

        val result = runCatching {
            val jsonText = context.assets.open(ASSET_FILE_NAME)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            parseJson(jsonText)
        }.getOrElse { emptyList() }

        cached = result
        return result
    }

    private fun parseJson(jsonText: String): List<BrandIrCodeSet> {
        val array = JSONArray(jsonText)
        val list = ArrayList<BrandIrCodeSet>(array.length())

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val protocolName = obj.getString("protocol")
            val protocol = runCatching { IrProtocol.valueOf(protocolName) }.getOrNull() ?: continue
            val address = obj.getInt("address")
            val extendedAddress = if (obj.isNull("extendedAddress")) null else obj.getInt("extendedAddress")
            val displayName = obj.optString("displayName", "Bilinmeyen Cihaz")

            val commandsObj = obj.getJSONObject("commands")
            val commands = LinkedHashMap<RemoteButton, Int>()
            commandsObj.keys().forEach { key ->
                val button = runCatching { RemoteButton.valueOf(key) }.getOrNull()
                if (button != null) {
                    commands[button] = commandsObj.getInt(key)
                }
            }
            if (commands.isEmpty() || commands[RemoteButton.POWER] == null) continue

            list += BrandIrCodeSet(
                brandKey = "lirc_blind_$i",
                displayNameEn = displayName,
                displayNameLocal = "LIRC veritabanı · ${protocol.name}",
                protocol = protocol,
                verified = false,
                address = address,
                extendedAddress = extendedAddress,
                commands = commands
            )
        }
        return list
    }

    companion object {
        private const val ASSET_FILE_NAME = "lirc_blind_scan.json"
    }
}
