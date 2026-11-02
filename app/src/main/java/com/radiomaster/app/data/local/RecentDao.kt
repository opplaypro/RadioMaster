package com.radiomaster.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RecentStationEntity)

    @Query("DELETE FROM recent_stations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM recent_stations")
    suspend fun deleteAll()

    @Query("SELECT station_json FROM recent_stations ORDER BY played_at DESC")
    fun getAll(): Flow<List<String>>

    @Query("DELETE FROM recent_stations WHERE played_at NOT IN (SELECT played_at FROM recent_stations ORDER BY played_at DESC LIMIT :limit)")
    suspend fun keepNewest(limit: Int)
}
