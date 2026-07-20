package com.atakolstudio.sure.data.local.dao

import androidx.room.*
import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedDeviceDao {

    @Query("SELECT * FROM saved_devices ORDER BY lastUsedEpochMillis DESC")
    fun observeAll(): Flow<List<SavedDeviceEntity>>

    @Query("SELECT * FROM saved_devices WHERE id = :id")
    suspend fun getById(id: Long): SavedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: SavedDeviceEntity): Long

    @Update
    suspend fun update(device: SavedDeviceEntity)

    @Delete
    suspend fun delete(device: SavedDeviceEntity)

    @Query("UPDATE saved_devices SET nickname = :newName WHERE id = :id")
    suspend fun rename(id: Long, newName: String)

    @Query("UPDATE saved_devices SET lastUsedEpochMillis = :timestamp WHERE id = :id")
    suspend fun touchLastUsed(id: Long, timestamp: Long)
}
