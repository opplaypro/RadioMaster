package com.radiomaster.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.put
import androidx.datastore.preferences.preferencesDataStore
import com.radiomaster.app.domain.model.AppTheme
import com.radiomaster.app.domain.repository.AppSettingsRepository
import com.radiomaster.app.domain.usecase.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import java.util.concurrent.TimeUnit

class AppSettingsRepositoryImpl(
    context: Context,
    private val defaultLocale: Locale
) : AppSettingsRepository {
    private val dataStore = context.preferencesDataStore(name = "radiomaster_prefs")

    private object Keys {
        val locale = stringPreferencesKey<>("locale")
        val theme = stringPreferencesKey<>("theme")
        val firstLaunch = booleanPreferencesKey<>("first_launch")
    }

    override val locale: Flow<Locale> = dataStore.data.map { prefs ->
        val tag = prefs[Keys.locale] ?: defaultLocale.toLanguageTag()
        LocaleHelper.toLocale(tag)
    }

    override suspend fun setLocale(locale: Locale) {
        dataStore.edit { put(Keys.locale, locale.toLanguageTag()) }
    }

    override val themeMode: Flow<AppTheme> = dataStore.data.map { prefs ->
        val value = prefs[Keys.theme] ?: AppTheme.SYSTEM.value
        AppTheme.values().firstOrNull { it.value == value } ?: AppTheme.SYSTEM
    }

    override suspend fun setThemeMode(theme: AppTheme) {
        dataStore.edit { put(Keys.theme, theme.value) }
    }

    override val isFirstLaunch: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.firstLaunch] ?: true
    }

    override suspend fun setFirstLaunch(value: Boolean) {
        dataStore.edit { put(Keys.firstLaunch, value) }
    }

    companion object {
        fun initialize(context: Context): AppSettingsRepositoryImpl =
            AppSettingsRepositoryImpl(context, Locale.getDefault())
    }
}
