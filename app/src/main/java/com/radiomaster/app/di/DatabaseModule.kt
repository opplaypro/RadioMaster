package com.radiomaster.app.di

import android.content.Context
import androidx.room.Room
import com.radiomaster.app.data.local.AppDatabase
import com.radiomaster.app.data.local.FavoriteDao
import com.radiomaster.app.data.local.RecentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "radiomaster_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    @Singleton
    fun provideRecentDao(database: AppDatabase): RecentDao = database.recentDao()
}
