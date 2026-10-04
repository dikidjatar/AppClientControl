package com.xeg911.appcontrol.di

import android.content.Context
import androidx.room.Room
import com.xeg911.appcontrol.data.local.AppDatabase
import com.xeg911.appcontrol.data.local.TransferHistoryDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideTransferHistoryDao(db: AppDatabase): TransferHistoryDao = db.transferHistoryDao()
}
