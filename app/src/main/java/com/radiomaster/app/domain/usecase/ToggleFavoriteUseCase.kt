package com.radiomaster.app.domain.usecase

import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow

class ToggleFavoriteUseCase(private val repository: StationRepository) {
    suspend operator fun invoke(station: Station) = repository.toggleFavorite(station)

    fun isFavorite(stationId: String): Flow<Boolean> = repository.getFavorites().map { it.any { it.id == stationId } }
}
