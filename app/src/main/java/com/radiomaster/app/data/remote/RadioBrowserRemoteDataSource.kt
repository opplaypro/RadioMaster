package com.radiomaster.app.data.remote

import com.radiomaster.app.domain.model.Station
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RadioBrowserRemoteDataSource(private val api: RadioBrowserApi) {
    suspend fun getPopularStations(limit: Int): List<Station> =
        withContext(Dispatchers.Default) {
            api.getTopStations(limit).map { it.toDomain() }
        }

    suspend fun searchStations(query: String, limit: Int): List<Station> =
        withContext(Dispatchers.Default) {
            api.searchStations(query, limit).map { it.toDomain() }
        }

    suspend fun stationsByCountry(country: String, limit: Int): List<Station> =
        withContext(Dispatchers.Default) {
            api.stationsByCountry(country, limit).map { it.toDomain() }
        }

    suspend fun stationsByLanguage(language: String, limit: Int): List<Station> =
        withContext(Dispatchers.Default) {
            api.stationsByLanguage(language, limit).map { it.toDomain() }
        }

    suspend fun stationsByTag(tag: String, limit: Int): List<Station> =
        withContext(Dispatchers.Default) {
            api.stationsByTag(tag, limit).map { it.toDomain() }
        }

    suspend fun stationById(uuid: String): Station? =
        withContext(Dispatchers.Default) {
            api.stationById(uuid)?.toDomain()
        }
}
