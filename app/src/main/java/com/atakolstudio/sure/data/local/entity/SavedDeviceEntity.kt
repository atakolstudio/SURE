package com.atakolstudio.sure.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Kullanıcının "Cihazlarım" ekranında kayıtlı gördüğü her bir uzaktan kumanda profili.
 */
@Entity(tableName = "saved_devices")
data class SavedDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,          // Kullanıcının verdiği özel isim (ör. "Salon TV'si")
    val brandKey: String,          // BrandIrDatabase.brandKey referansı
    val brandDisplayName: String,
    val model: String? = null,
    val deviceType: String,        // DeviceType.name
    val connectionType: String,    // ConnectionType.name
    val createdAtEpochMillis: Long,
    val lastUsedEpochMillis: Long
)
