package com.atakolstudio.sure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Kullanicinin "Cihazlarim" ekraninda kayitli gordugu her bir uzaktan kumanda profili.
 *
 * Iki tur cihaz olabilir:
 * 1) Bilinen marka: brandKey, BrandIrDatabase.brands icindeki bir anahtara isaret eder,
 *    custom* alanlari null'dur.
 * 2) Manuel/ozel bulunan cihaz: brandKey = "custom" olur, IR protokolu ve komutlari
 *    custom* alanlarinda saklanir (kullanici "Manuel Bul" akisiyla kendi cihazini
 *    bulup kaydettiginde olusur).
 */
@Entity(tableName = "saved_devices")
data class SavedDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,          // Kullanicinin verdigi ozel isim (or. "Salon TV'si")
    val brandKey: String,          // BrandIrDatabase.brandKey referansi VEYA "custom"
    val brandDisplayName: String,
    val model: String? = null,
    val deviceType: String,        // DeviceType.name
    val connectionType: String,    // ConnectionType.name
    val createdAtEpochMillis: Long,
    val lastUsedEpochMillis: Long,
    // --- Sadece brandKey == "custom" oldugunda dolu olan alanlar ---
    val customProtocol: String? = null,        // IrProtocol.name
    val customAddress: Int? = null,
    val customExtendedAddress: Int? = null,
    val customCommandsJson: String? = null     // {"POWER": 18, "VOLUME_UP": 26, ...}
)
