package com.radiomaster.app.domain.usecase

import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow

class AddRecentUseCase(private val repository: StationRepository) {
    suspend operator fun invoke(station: Station) = repository.addRecent(station)
    fun recent(): Flow<List<Station>> = repository.getRecent()
}
