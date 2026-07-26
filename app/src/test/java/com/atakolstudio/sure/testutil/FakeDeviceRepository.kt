package com.atakolstudio.sure.testutil

import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity
import com.atakolstudio.sure.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Gerçek Room veritabanı olmadan ViewModel testleri yazabilmek için basit,
 * bellek-içi (in-memory) bir [DeviceRepository] sahtesi (fake).
 */
class FakeDeviceRepository : DeviceRepository {

    private val devicesFlow = MutableStateFlow<List<SavedDeviceEntity>>(emptyList())
    private var nextId = 1L

    val currentDevices: List<SavedDeviceEntity> get() = devicesFlow.value

    override fun observeDevices(): StateFlow<List<SavedDeviceEntity>> = devicesFlow

    override suspend fun getDevice(id: Long): SavedDeviceEntity? =
        devicesFlow.value.find { it.id == id }

    override suspend fun addDevice(device: SavedDeviceEntity): Long {
        val id = nextId++
        val withId = device.copy(id = id)
        devicesFlow.value = devicesFlow.value + withId
        return id
    }

    override suspend fun renameDevice(id: Long, newName: String) {
        devicesFlow.value = devicesFlow.value.map {
            if (it.id == id) it.copy(nickname = newName) else it
        }
    }

    override suspend fun deleteDevice(device: SavedDeviceEntity) {
        devicesFlow.value = devicesFlow.value.filterNot { it.id == device.id }
    }

    override suspend fun touchLastUsed(id: Long) {
        devicesFlow.value = devicesFlow.value.map {
            if (it.id == id) it.copy(lastUsedEpochMillis = System.currentTimeMillis()) else it
        }
    }
}
