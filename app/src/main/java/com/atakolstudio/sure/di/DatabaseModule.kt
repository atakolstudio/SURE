package com.atakolstudio.sure.di

import android.content.Context
import androidx.room.Room
import com.atakolstudio.sure.data.local.SureDatabase
import com.atakolstudio.sure.data.local.dao.SavedDeviceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSureDatabase(@ApplicationContext context: Context): SureDatabase =
        Room.databaseBuilder(context, SureDatabase::class.java, SureDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideSavedDeviceDao(database: SureDatabase): SavedDeviceDao =
        database.savedDeviceDao()
}
