package com.atakolstudio.sure.data.repository

import com.atakolstudio.sure.data.local.dao.SavedDeviceDao
import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface DeviceRepository {
    fun observeDevices(): Flow<List<SavedDeviceEntity>>
    suspend fun getDevice(id: Long): SavedDeviceEntity?
    suspend fun addDevice(device: SavedDeviceEntity): Long
    suspend fun renameDevice(id: Long, newName: String)
    suspend fun deleteDevice(device: SavedDeviceEntity)
    suspend fun touchLastUsed(id: Long)
}

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val dao: SavedDeviceDao
) : DeviceRepository {

    override fun observeDevices(): Flow<List<SavedDeviceEntity>> = dao.observeAll()

    override suspend fun getDevice(id: Long): SavedDeviceEntity? = dao.getById(id)

    override suspend fun addDevice(device: SavedDeviceEntity): Long = dao.insert(device)

    override suspend fun renameDevice(id: Long, newName: String) = dao.rename(id, newName)

    override suspend fun deleteDevice(device: SavedDeviceEntity) = dao.delete(device)

    override suspend fun touchLastUsed(id: Long) =
        dao.touchLastUsed(id, System.currentTimeMillis())
}
