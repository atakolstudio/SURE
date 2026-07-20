package com.atakolstudio.sure.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.atakolstudio.sure.data.local.dao.SavedDeviceDao
import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity

@Database(
    entities = [SavedDeviceEntity::class],
    version = 1,
    exportSchema = true
)
abstract class SureDatabase : RoomDatabase() {
    abstract fun savedDeviceDao(): SavedDeviceDao

    companion object {
        const val DATABASE_NAME = "sure_database"
    }
}
