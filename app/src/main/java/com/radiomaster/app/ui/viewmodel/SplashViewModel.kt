package com.radiomaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.radiomaster.app.domain.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val settings: AppSettingsRepository
) : ViewModel() {

    val isFirstLaunch = settings.isFirstLaunch
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
}
