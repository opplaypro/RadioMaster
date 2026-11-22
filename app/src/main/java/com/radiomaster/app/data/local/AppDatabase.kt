package com.radiomaster.app.data.local

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.radiomaster.app.domain.model.Station
import kotlinx.coroutines.flow.Flow

/* ------------------------------------------------------------------- rows */

/**
 * A station, flattened into non-null columns so that Room needs no type converters and the
 * favorites/history queries stay plain SQL. `""` / `0.0` mean "unknown", which is exactly how
 * radio-browser.info reports them.
 */
@Entity(tableName = "stations")
data class StationRow(
    @PrimaryKey @ColumnInfo(name = "station_uuid") val stationUuid: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "url_resolved") val urlResolved: String,
    @ColumnInfo(name = "homepage") val homepage: String,
    @ColumnInfo(name = "favicon") val favicon: String,
    @ColumnInfo(name = "country") val country: String,
    @ColumnInfo(name = "country_code") val countryCode: String,
    @ColumnInfo(name = "state") val state: String,
    @ColumnInfo(name = "language") val language: String,
    @ColumnInfo(name = "tags") val tags: String,
    @ColumnInfo(name = "codec") val codec: String,
    @ColumnInfo(name = "bitrate") val bitrateKbps: Int,
    @ColumnInfo(name = "votes") val votes: Int,
    @ColumnInfo(name = "clickcount") val clickCount: Int,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
)

fun StationRow.toStation(): Station = Station(
    id = stationUuid,
    name = name,
    streamUrl = urlResolved.ifBlank { url },
    homepage = homepage.ifBlank { null },
    faviconUrl = favicon.ifBlank { null },
    country = country.ifBlank { null },
    countryCode = countryCode.ifBlank { null },
    state = state.ifBlank { null },
    language = language.ifBlank { null },
    tags = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() },
    codec = codec.ifBlank { null },
    bitrateKbps = bitrateKbps.takeIf { it > 0 },
    votes = votes,
    clicksLastDay = clickCount,
    latitude = latitude.takeIf { it != 0.0 },
    longitude = longitude.takeIf { it != 0.0 },
)

fun Station.toRow(): StationRow = StationRow(
    stationUuid = id,
    name = name,
    url = streamUrl,
    urlResolved = streamUrl,
    homepage = homepage.orEmpty(),
    favicon = faviconUrl.orEmpty(),
    country = country.orEmpty(),
    countryCode = countryCode.orEmpty(),
    state = state.orEmpty(),
    language = language.orEmpty(),
    tags = tags.joinToString(","),
    codec = codec.orEmpty(),
    bitrateKbps = bitrateKbps ?: 0,
    votes = votes,
    clickCount = clicksLastDay,
    latitude = latitude ?: 0.0,
    longitude = longitude ?: 0.0,
)

@Entity(tableName = "favorites")
data class FavoriteEntry(
    @PrimaryKey @ColumnInfo(name = "station_uuid") val stationUuid: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey @ColumnInfo(name = "station_uuid") val stationUuid: String,
    /** Last playback; also the ordering key of the history list. */
    @ColumnInfo(name = "played_at") val playedAt: Long,
    @ColumnInfo(name = "play_count") val playCount: Int = 1,
)

/* -------------------------------------------------------------------- dao */

@Dao
interface StationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: StationRow)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<StationRow>)

    @Query("SELECT * FROM stations WHERE station_uuid = :uuid")
    suspend fun byId(uuid: String): StationRow?

    /* favorites */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entry: FavoriteEntry)

    @Query("DELETE FROM favorites WHERE station_uuid = :uuid")
    suspend fun removeFavorite(uuid: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE station_uuid = :uuid)")
    fun isFavorite(uuid: String): Flow<Boolean>

    @Query(
        """
        SELECT s.* FROM stations s
        INNER JOIN favorites f ON f.station_uuid = s.station_uuid
        ORDER BY f.added_at DESC
        """,
    )
    fun favoriteStations(): Flow<List<StationRow>>

    @Query(
        """
        SELECT s.* FROM stations s
        INNER JOIN favorites f ON f.station_uuid = s.station_uuid
        ORDER BY f.added_at DESC
        """,
    )
    suspend fun favoriteStationsOnce(): List<StationRow>

    /* history */

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntry)

    @Query("SELECT play_count FROM history WHERE station_uuid = :uuid")
    suspend fun playCount(uuid: String): Int?

    /**
     * Read-and-increment inside a single transaction. SQLite `UPSERT` would be one statement,
     * but it needs SQLite 3.24 while API 26 ships 3.19, so the write stays transactional
     * instead — that is what makes concurrent plays safe.
     */
    @Transaction
    suspend fun touchHistory(uuid: String, playedAt: Long) {
        val previous = playCount(uuid)
        insertHistory(
            HistoryEntry(stationUuid = uuid, playedAt = playedAt, playCount = (previous ?: 0) + 1),
        )
    }

    @Query(
        """
        SELECT s.* FROM stations s
        INNER JOIN history h ON h.station_uuid = s.station_uuid
        ORDER BY h.played_at DESC
        LIMIT :limit
        """,
    )
    fun historyStations(limit: Int): Flow<List<StationRow>>

    @Query(
        """
        SELECT s.* FROM stations s
        INNER JOIN history h ON h.station_uuid = s.station_uuid
        ORDER BY h.played_at DESC
        LIMIT :limit
        """,
    )
    suspend fun historyStationsOnce(limit: Int): List<StationRow>

    /** Keeps the table bounded; `LIMIT` inside a sub-query is valid SQLite. */
    @Query(
        """
        DELETE FROM history WHERE station_uuid NOT IN (
            SELECT station_uuid FROM history ORDER BY played_at DESC LIMIT :keep
        )
        """,
    )
    suspend fun trimHistory(keep: Int)

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}

/* ---------------------------------------------------------------- database */

@Database(
    entities = [StationRow::class, FavoriteEntry::class, HistoryEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao

    companion object {
        const val NAME: String = "radiomaster.db"
        const val HISTORY_LIMIT: Int = 200
    }
}

/**
 * No migrations are registered on purpose: version 1 has no predecessor, and a
 * `fallbackToDestructiveMigration` flag would silently wipe favorites in the first release that
 * changes the schema. Add a real Migration together with the next version bump.
 */
internal fun createAppDatabase(context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()
