package com.radiomaster.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_stations")
data class FavoriteStationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "station_json") val stationJson: String,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)
