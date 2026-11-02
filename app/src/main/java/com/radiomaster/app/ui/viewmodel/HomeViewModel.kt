package com.radiomaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.viewModelScope
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.StationRepository
import com.radiomaster.app.domain.usecase.AddRecentUseCase
import com.radiomaster.app.media.PlaybackState
import com.radiomaster.app.media.RadioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StationRepository,
    private val playerManager: RadioPlayerManager,
    private val addRecentUseCase: AddRecentUseCase
) : ViewModel() {

    val popularStations = repository.getPopularStations(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentStations = repository.getRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState = playerManager.playbackState
    val currentStation = playerManager.currentStation

    val favoriteIds = repository.getFavorites()
        .map { it.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun playStation(station: Station) = viewModelScope.launch {
        addRecentUseCase(station)
        playerManager.play(station)
    }

    fun toggleFavorite(station: Station) = viewModelScope.launch {
        repository.toggleFavorite(station)
    }

    val isPlayingFor: (Station) -> Boolean = { station ->
        playerManager.currentStation.value?.id == station.id && playerManager.isPlaying
    }
}
