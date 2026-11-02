package com.radiomaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.viewModelScope
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.StationRepository
import com.radiomaster.app.media.RadioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: StationRepository,
    private val playerManager: RadioPlayerManager
) : ViewModel() {

    val favorites = repository.getFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeFavorite(station: Station) = viewModelScope.launch {
        repository.toggleFavorite(station)
    }

    fun playStation(station: Station) = viewModelScope.launch {
        playerManager.play(station)
    }

    val playbackState = playerManager.playbackState
    val currentStation = playerManager.currentStation
}
