package com.radiomaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.viewModelScope
import com.radiomaster.app.domain.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: AppSettingsRepository
) : ViewModel() {

    val locale = settings.locale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Locale.getDefault())

    val themeMode = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.radiomaster.app.domain.model.AppTheme.SYSTEM)

    /** Emituje nowy język, gdy użytkownik go zmieni – aktywność powinna się zrecreate'ować. */
    val localeChangeRequested = MutableSharedFlow<Locale>(replay = 1)

    fun setLanguage(locale: Locale) = viewModelScope.launch {
        settings.setLocale(locale)
        localeChangeRequested.emit(locale)
    }

    fun setTheme(theme: com.radiomaster.app.domain.model.AppTheme) = viewModelScope.launch {
        settings.setThemeMode(theme)
    }
}
