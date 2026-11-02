package com.radiomaster.app.domain.repository

import com.radiomaster.app.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow
import java.util.Locale

interface AppSettingsRepository {
    val locale: Flow<Locale>
    fun setLocale(locale: Locale)
    val themeMode: Flow<AppTheme>
    fun setThemeMode(theme: AppTheme)
    val isFirstLaunch: Flow<Boolean>
    fun setFirstLaunch(value: Boolean)
}
