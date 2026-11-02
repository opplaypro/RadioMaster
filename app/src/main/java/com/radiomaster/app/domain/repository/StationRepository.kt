package com.radiomaster.app.domain.repository

import com.radiomaster.app.domain.model.Station
import kotlinx.coroutines.flow.Flow

interface StationRepository {
    /** Stacje polecane (najpopularniejsze na całym świecie). */
    fun getPopularStations(limit: Int = 100): Flow<List<Station>>
    /** Wyszukiwanie stacji po nazwie, kraju lub tagu. */
    fun searchStations(query: String, limit: Int = 30): Flow<List<Station>>
    /** Stacje z konkretnego kraju. */
    fun stationsByCountry(country: String, limit: Int = 50): Flow<List<Station>>
    /** Stacje z konkretnego języka. */
    fun stationsByLanguage(language: String, limit: Int = 50): Flow<List<Station>>
    /** Stacje po tagu (gatunku). */
    fun stationsByTag(tag: String, limit: Int = 50): Flow<List<Station>>
    fun getStationById(id: String): Station?
    fun getFavorites(): Flow<List<Station>>
    fun toggleFavorite(station: Station)
    fun getRecent(): Flow<List<Station>>
    fun addRecent(station: Station)
    fun clearRecent()
}
