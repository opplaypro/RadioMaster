package com.radiomaster.app.data.repository

import com.radiomaster.app.data.local.AppDatabase
import com.radiomaster.app.data.local.FavoriteEntry
import com.radiomaster.app.data.local.StationDao
import com.radiomaster.app.data.local.StationRow
import com.radiomaster.app.data.local.toRow
import com.radiomaster.app.data.local.toStation
import com.radiomaster.app.data.remote.RadioBrowserClient
import com.radiomaster.app.domain.model.Country
import com.radiomaster.app.domain.model.Genre
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.LocalRepository
import com.radiomaster.app.domain.repository.StationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Remote catalogue, with every fetched station mirrored into Room so favorites survive. */
class StationRepositoryImpl(
    private val client: RadioBrowserClient,
    private val dao: StationDao,
) : StationRepository {

    private suspend fun List<Station>.cache(): List<Station> = also { rows ->
        runCatching { dao.upsertAll(rows.map { it.toRow() }) } // cache writes are non-essential
    }

    override suspend fun search(query: String?, country: String?, genre: String?, limit: Int, offset: Int) =
        client.search(query = query, country = country, tag = genre, limit = limit, offset = offset).cache()

    override suspend fun popular(limit: Int) = client.popular(limit).cache()

    override suspend fun topVoted(limit: Int) = client.topVoted(limit).cache()

    override suspend fun byCountry(country: String, limit: Int) = client.byCountry(country, limit).cache()

    override suspend fun byTag(tag: String, limit: Int) = client.byTag(tag, limit).cache()

    override suspend fun random(countryCode: String?) =
        client.random(countryCode)?.also { station -> runCatching { dao.upsert(station.toRow()) } }

    override suspend fun nearby(latitude: Double, longitude: Double, radiusKm: Double, limit: Int) =
        client.nearby(latitude, longitude, radiusKm, limit).cache()

    override suspend fun countries(): List<Country> = client.countries()

    override suspend fun genres(): List<Genre> = client.genres()

    override suspend fun reportPlay(stationId: String) = client.countClick(stationId)
}

/** Favorites + playback history. Reads are cold-flow-backed by Room and survive process death. */
class LocalRepositoryImpl(private val dao: StationDao) : LocalRepository {

    override fun observeFavorites(): Flow<List<Station>> = dao.favoriteStations().mapRows()

    override suspend fun favorites(): List<Station> = dao.favoriteStationsOnce().map { it.toStation() }

    override fun isFavorite(stationId: String): Flow<Boolean> = dao.isFavorite(stationId)

    override suspend fun setFavorite(station: Station, favorite: Boolean) {
        if (favorite) addFavorite(station) else removeFavorite(station.id)
    }

    override suspend fun addFavorite(station: Station) {
        dao.upsert(station.toRow())
        dao.addFavorite(FavoriteEntry(stationUuid = station.id, addedAt = System.currentTimeMillis()))
    }

    override suspend fun removeFavorite(stationId: String) = dao.removeFavorite(stationId)

    override suspend fun toggleFavorite(station: Station): Boolean {
        val nowFavorite = favorites().none { it.id == station.id }
        setFavorite(station, nowFavorite)
        return nowFavorite
    }

    override fun observeHistory(limit: Int): Flow<List<Station>> = dao.historyStations(limit).mapRows()

    override suspend fun history(limit: Int): List<Station> = dao.historyStationsOnce(limit).map { it.toStation() }

    override suspend fun recordPlay(station: Station) {
        dao.upsert(station.toRow())
        dao.touchHistory(station.id, System.currentTimeMillis())
        dao.trimHistory(AppDatabase.HISTORY_LIMIT)
    }

    override suspend fun clearHistory() = dao.clearHistory()

    override suspend fun stationById(id: String): Station? = dao.byId(id)?.toStation()

    private fun Flow<List<StationRow>>.mapRows(): Flow<List<Station>> =
        map { rows -> rows.map { it.toStation() } }
}
