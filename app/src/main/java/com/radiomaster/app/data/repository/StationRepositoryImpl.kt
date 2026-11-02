package com.radiomaster.app.data.repository

import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.StationRepository
import com.radiomaster.app.data.local.LocalDataSource
import com.radiomaster.app.data.remote.RadioBrowserRemoteDataSource
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class StationRepositoryImpl(
    private val remote: RadioBrowserRemoteDataSource,
    private val local: LocalDataSource
) : StationRepository {
    override fun getPopularStations(limit: Int) = remote.getPopularStationsFlow(limit)

    override fun searchStations(query: String, limit: Int) =
        remote.searchStationsFlow(query, limit)

    override fun stationsByCountry(country: String, limit: Int) =
        remote.stationsByCountryFlow(country, limit)

    override fun stationsByLanguage(language: String, limit: Int) =
        remote.stationsByLanguageFlow(language, limit)

    override fun stationsByTag(tag: String, limit: Int) =
        remote.stationsByTagFlow(tag, limit)

    override suspend fun getStationById(id: String): Station? =
        remote.stationById(id)?.toDomain()

    override fun getFavorites() = local.observeFavorites()

    override fun toggleFavorite(station: Station) = runBlocking { local.toggleFavorite(station) }

    override fun getRecent() = local.observeRecent()

    override fun addRecent(station: Station) = runBlocking { local.addRecent(station) }

    override fun clearRecent() = runBlocking { local.clearRecent() }
}

// Rozszerzenia źródła zdalnego zwracające Flowy (warstwa danych enkapsuluje wywołania suspend).
private fun RadioBrowserRemoteDataSource.getPopularStationsFlow(limit: Int) =
    kotlinx.coroutines.flow.flow {
        emit(getPopularStations(limit))
    }

private fun RadioBrowserRemoteDataSource.searchStationsFlow(query: String, limit: Int) =
    kotlinx.coroutines.flow.flow {
        if (query.isNotBlank()) emit(searchStations(query, limit))
    }

private fun RadioBrowserRemoteDataSource.stationsByCountryFlow(country: String, limit: Int) =
    kotlinx.coroutines.flow.flow {
        emit(stationsByCountry(country, limit))
    }

private fun RadioBrowserRemoteDataSource.stationsByLanguageFlow(language: String, limit: Int) =
    kotlinx.coroutines.flow.flow {
        emit(stationsByLanguage(language, limit))
    }

private fun RadioBrowserRemoteDataSource.stationsByTagFlow(tag: String, limit: Int) =
    kotlinx.coroutines.flow.flow {
        emit(stationsByTag(tag, limit))
    }
