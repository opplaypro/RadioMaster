package com.radiomaster.app.domain.usecase

import com.radiomaster.app.domain.model.BrowseIds
import com.radiomaster.app.domain.model.Country
import com.radiomaster.app.domain.model.Genre
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.LocalRepository
import com.radiomaster.app.domain.repository.StationRepository

/**
 * Resolves one browse id into the stations it contains.
 *
 * This is the one place that knows the browse topology, and both front ends go through it: the
 * phone screens (so a chip on the home page and a node in the car show the same list) and the
 * `MediaLibrarySession` browse tree for Android Auto. Local collections are sliced here because
 * Room answers the whole list, while remote ones are requested with the offset the caller gave.
 */
class StationBrowser(
    private val stations: StationRepository,
    private val local: LocalRepository,
) {
    suspend fun countries(): List<Country> = stations.countries()

    suspend fun genres(): List<Genre> = stations.genres()

    suspend fun stationsOf(browseId: String, limit: Int = DEFAULT_LIMIT, offset: Int = 0): List<Station> =
        when (browseId) {
            BrowseIds.ROOT -> emptyList()
            BrowseIds.FAVORITES -> local.favorites().window(offset, limit)
            BrowseIds.HISTORY -> local.history(limit = HISTORY_FETCH).window(offset, limit)
            BrowseIds.POPULAR -> stations.popular(limit = fetchSize(offset, limit)).window(offset, limit)
            BrowseIds.TOP_VOTED -> stations.topVoted(limit = fetchSize(offset, limit)).window(offset, limit)
            BrowseIds.RANDOM -> listOfNotNull(stations.random()).window(offset, limit)
            else -> BrowseIds.countryOf(browseId)?.let { stations.byCountry(it, fetchSize(offset, limit)).window(offset, limit) }
                ?: BrowseIds.genreOf(browseId)?.let { stations.byTag(it, fetchSize(offset, limit)).window(offset, limit) }
                ?: emptyList()
        }

    suspend fun search(query: String, country: String? = null, genre: String? = null, limit: Int = DEFAULT_LIMIT, offset: Int = 0) =
        stations.search(query = query, country = country, genre = genre, limit = limit, offset = offset)

    suspend fun nearby(latitude: Double, longitude: Double, radiusKm: Double, limit: Int = NEARBY_LIMIT) =
        stations.nearby(latitude, longitude, radiusKm, limit)

    /**
     * Resolves a station referenced by a media id. Room caches every record the app has ever
     * fetched, so any id the car got out of the browse tree is guaranteed to be found here —
     * a cold id (cleared cache) simply returns null and the caller skips it.
     */
    suspend fun stationById(id: String): Station? = local.stationById(id)

    /**
     * Requests at most [MAX_FETCH] records so a browse page can never pull the whole directory
     * into memory; the API caps a page anyway, this just bounds the offset arithmetic.
     */
    private fun fetchSize(offset: Int, limit: Int): Int = (offset + limit).coerceAtMost(MAX_FETCH)

    private fun List<Station>.window(offset: Int, limit: Int): List<Station> =
        if (offset <= 0) take(limit) else drop(offset).take(limit)

    companion object {
        const val DEFAULT_LIMIT = 60
        const val NEARBY_LIMIT = 300
        private const val HISTORY_FETCH = 200
        private const val MAX_FETCH = 300
    }
}
