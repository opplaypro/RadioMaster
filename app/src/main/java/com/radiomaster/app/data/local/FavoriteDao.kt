package com.radiomaster.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteStationEntity)

    @Delete
    suspend fun delete(entity: FavoriteStationEntity)

    @Query("DELETE FROM favorite_stations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_stations WHERE id = :id LIMIT 1)")
    suspend fun exists(id: String): Boolean

    @Query("SELECT station_json FROM favorite_stations ORDER BY added_at DESC")
    fun getAll(): Flow<List<String>>
}
