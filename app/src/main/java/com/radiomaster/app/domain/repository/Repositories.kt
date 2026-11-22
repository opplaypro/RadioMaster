package com.radiomaster.app.domain.repository

import com.radiomaster.app.domain.model.Country
import com.radiomaster.app.domain.model.Genre
import com.radiomaster.app.domain.model.Station
import kotlinx.coroutines.flow.Flow

/**
 * The remote catalogue (radio-browser.info). All methods are suspending and return plain lists:
 * paging is explicit (`limit`/`offset`) because the directory is a REST search, not a stream.
 */
interface StationRepository {
    suspend fun search(
        query: String? = null,
        country: String? = null,
        genre: String? = null,
        limit: Int = 60,
        offset: Int = 0,
    ): List<Station>

    suspend fun popular(limit: Int = 60): List<Station>
    suspend fun topVoted(limit: Int = 60): List<Station>
    suspend fun byCountry(country: String, limit: Int = 100): List<Station>
    suspend fun byTag(tag: String, limit: Int = 100): List<Station>
    suspend fun random(countryCode: String? = null): Station?
    suspend fun nearby(latitude: Double, longitude: Double, radiusKm: Double, limit: Int = 300): List<Station>

    suspend fun countries(): List<Country>
    suspend fun genres(): List<Genre>

    /** Tells the directory that this station started playing; best effort, never throws. */
    suspend fun reportPlay(stationId: String)
}

/** Favorites and playback history, persisted on the device. */
interface LocalRepository {
    fun observeFavorites(): Flow<List<Station>>
    suspend fun favorites(): List<Station>
    fun isFavorite(stationId: String): Flow<Boolean>
    suspend fun setFavorite(station: Station, favorite: Boolean)
    suspend fun addFavorite(station: Station)
    suspend fun removeFavorite(stationId: String)
    suspend fun toggleFavorite(station: Station): Boolean

    fun observeHistory(limit: Int = 100): Flow<List<Station>>
    suspend fun history(limit: Int = 100): List<Station>
    suspend fun recordPlay(station: Station)
    suspend fun clearHistory()

    suspend fun stationById(id: String): Station?
}

/** User-visible preferences. */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val appLanguage: Flow<AppLanguage>
    val onboarded: Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAppLanguage(language: AppLanguage)
    suspend fun setOnboarded()
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** `SYSTEM` follows the device locale, the others force the app language. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    POLISH("pl"),
}
