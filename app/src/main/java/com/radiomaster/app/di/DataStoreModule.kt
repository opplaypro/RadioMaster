package com.radiomaster.app.di

import android.content.Context
import com.radiomaster.app.data.repository.AppSettingsRepositoryImpl
import com.radiomaster.app.domain.repository.AppSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideAppSettingsRepository(context: Context): AppSettingsRepository =
        AppSettingsRepositoryImpl.initialize(context)
}
