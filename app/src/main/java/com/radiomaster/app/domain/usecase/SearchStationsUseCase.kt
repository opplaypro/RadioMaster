package com.radiomaster.app.domain.usecase

import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow

class SearchStationsUseCase(private val repository: StationRepository) {
    suspend operator fun invoke(query: String, limit: Int = 30): List<Station> =
        repository.searchStations(query, limit).first()

    fun searchFlow(query: String, limit: Int = 30): Flow<List<Station>> =
        repository.searchStations(query, limit).debounce(300.0)
}
