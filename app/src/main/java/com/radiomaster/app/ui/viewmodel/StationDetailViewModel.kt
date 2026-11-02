package com.radiomaster.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.viewModelScope
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.StationRepository
import com.radiomaster.app.domain.usecase.AddRecentUseCase
import com.radiomaster.app.domain.usecase.ToggleFavoriteUseCase
import com.radiomaster.app.media.RadioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StationDetailViewModel @Inject constructor(
    private val repository: StationRepository,
    private val playerManager: RadioPlayerManager,
    private val addRecentUseCase: AddRecentUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _station = MutableStateFlow<Station?>(null)
    val station = _station.asStateFlow()

    fun loadStation(stationId: String) = viewModelScope.launch {
        _station.value = repository.getStationById(stationId)
    }

    fun play() = _station.value?.let { station ->
        viewModelScope.launch {
            addRecentUseCase(station)
            playerManager.play(station)
        }
    }

    fun togglePlayback() = playerManager.toggle()

    fun toggleFavorite() = _station.value?.let { station ->
        viewModelScope.launch { toggleFavoriteUseCase(station) }
    }

    val isFavoriteFlow: (String) -> kotlinx.coroutines.flow.Flow<Boolean> = { stationId ->
        toggleFavoriteUseCase.isFavorite(stationId)
    }

    val playbackState = playerManager.playbackState

    val isPlaying: StateFlow<Boolean> = playerManager.currentStation
        .map { it != null && playerManager.isPlaying }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentStationId: StateFlow<String?> = playerManager.currentStation
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
