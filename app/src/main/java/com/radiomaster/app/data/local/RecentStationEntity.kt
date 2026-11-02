package com.radiomaster.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_stations")
data class RecentStationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "station_json") val stationJson: String,
    @ColumnInfo(name = "played_at") val playedAt: Long = System.currentTimeMillis()
)
