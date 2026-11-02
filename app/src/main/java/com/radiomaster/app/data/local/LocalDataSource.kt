package com.radiomaster.app.data.local

import com.google.gson.Gson
import com.radiomaster.app.data.remote.StationDto
import com.radiomaster.app.domain.model.Station
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class LocalDataSource(
    private val database: AppDatabase,
    private val gson: Gson,
    private val maxRecent: Int = 50
) {
    suspend fun getFavorites(): List<Station> =
        database.favoriteDao().getAll().first().map { json ->
            gson.fromJson(json, StationDto::class.java).toDomain(isFavorite = true)
        }

    suspend fun toggleFavorite(station: Station) {
        val dao = database.favoriteDao()
        if (dao.exists(station.id)) {
            dao.deleteById(station.id)
        } else {
            dao.insert(FavoriteStationEntity(id = station.id, stationJson = gson.toJson(station)))
        }
    }

    suspend fun isFavorite(id: String): Boolean = database.favoriteDao().exists(id)

    suspend fun addRecent(station: Station) {
        val dao = database.recentDao()
        dao.insert(RecentStationEntity(id = station.id, stationJson = gson.toJson(station)))
        dao.keepNewest(maxRecent)
    }

    suspend fun getRecent(): List<Station> =
        database.recentDao().getAll().first().map { json ->
            gson.fromJson(json, StationDto::class.java).toDomain(isRecent = true)
        }.sortedByDescending { it.playedAt ?: 0 }

    suspend fun clearRecent() = database.recentDao().deleteAll()

    fun observeFavorites(): Flow<List<Station>> = database.favoriteDao().getAll().map { jsonList ->
        jsonList.map { gson.fromJson(it, StationDto::class.java).toDomain(isFavorite = true) }
    }.flowOn(Dispatchers.IO)

    fun observeRecent(): Flow<List<Station>> = database.recentDao().getAll().map { jsonList ->
        jsonList.map { gson.fromJson(it, StationDto::class.java).toDomain(isRecent = true) }
    }.flowOn(Dispatchers.IO)
}
