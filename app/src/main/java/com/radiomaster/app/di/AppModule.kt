package com.radiomaster.app.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.radiomaster.app.data.local.LocalDataSource
import com.radiomaster.app.data.local.AppDatabase
import com.radiomaster.app.data.repository.StationRepositoryImpl
import com.radiomaster.app.domain.repository.StationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    fun provideLocalDataSource(
        database: AppDatabase,
        gson: Gson
    ): LocalDataSource = LocalDataSource(database, gson)

    @Provides
    @Singleton
    fun provideStationRepository(
        remoteDataSource: com.radiomaster.app.data.remote.RadioBrowserRemoteDataSource,
        localDataSource: LocalDataSource
    ): StationRepository = StationRepositoryImpl(remoteDataSource, localDataSource)
}
