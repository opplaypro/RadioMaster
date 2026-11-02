package com.radiomaster.app.presentation

import android.app.Application
import com.radiomaster.app.domain.repository.AppSettingsRepository
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject lateinit var settingsRepository: AppSettingsRepository

    /** Aktualny język aplikacji (wczytywany asynchronicznie z DataStore). */
    var currentLocale: Locale = Locale.getDefault()
        private set

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        MainScope().launch {
            settingsRepository.locale.collect { locale ->
                currentLocale = locale
            }
        }
    }
}
