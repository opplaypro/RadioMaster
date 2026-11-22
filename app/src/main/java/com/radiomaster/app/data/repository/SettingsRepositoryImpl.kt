package com.radiomaster.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.radiomaster.app.domain.repository.AppLanguage
import com.radiomaster.app.domain.repository.SettingsRepository
import com.radiomaster.app.domain.repository.ThemeMode
import com.radiomaster.app.locale.AppLocale
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Preferences in DataStore. Reads never throw: a corrupt file degrades to defaults through
 * [emptyPreferences] rather than crashing the app at startup.
 */
class SettingsRepositoryImpl(private val context: Context) : SettingsRepository {

    private val data: Flow<Preferences> = context.settingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    override val themeMode: Flow<ThemeMode> = data.map { preferences ->
        when (preferences[KEY_THEME]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    override val appLanguage: Flow<AppLanguage> = data.map { preferences ->
        when (preferences[KEY_LANGUAGE]) {
            AppLanguage.ENGLISH.tag -> AppLanguage.ENGLISH
            AppLanguage.POLISH.tag -> AppLanguage.POLISH
            else -> AppLanguage.SYSTEM
        }
    }

    override val onboarded: Flow<Boolean> = data.map { preferences -> preferences[KEY_ONBOARDED] ?: false }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[KEY_THEME] = mode.name }
    }

    override suspend fun setAppLanguage(language: AppLanguage) {
        // DataStore drives the UI; AppLocale is the synchronous mirror the platform needs, and on
        // API 33+ it is the framework per-app locale setting itself.
        AppLocale.set(context, language.tag.takeIf { it.isNotEmpty() })
        context.settingsDataStore.edit { it[KEY_LANGUAGE] = language.tag }
    }

    override suspend fun setOnboarded() {
        context.settingsDataStore.edit { it[KEY_ONBOARDED] = true }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_ONBOARDED = booleanPreferencesKey("onboarded")
    }
}
