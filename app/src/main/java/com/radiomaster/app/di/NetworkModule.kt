package com.radiomaster.app.di

import com.radiomaster.app.data.remote.RadioBrowserApi
import com.radiomaster.app.data.remote.RadioBrowserRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import kotlin.concurrent.duration.seconds

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message -> /* Timber.d(message) */ }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30.seconds)
            .readTimeout(30.seconds)
            .writeTimeout(30.seconds)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://all.api.radio-browser.info/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideRadioBrowserApi(retrofit: Retrofit): RadioBrowserApi =
        retrofit.create(RadioBrowserApi::class.java)

    @Provides
    @Singleton
    fun provideRemoteDataSource(api: RadioBrowserApi): RadioBrowserRemoteDataSource =
        RadioBrowserRemoteDataSource(api)
}
