package com.radiomaster.app.data.remote

import com.radiomaster.app.domain.model.Country
import com.radiomaster.app.domain.model.Genre
import com.radiomaster.app.domain.model.Station
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Talks to radio-browser.info and maps records to [Station]s.
 *
 * The public API is a pool of community mirrors. Any one of them can be down or rate-limiting, so
 * [hosts] is tried in order and the first answer wins. Directory lists (countries, tags) change a
 * few times a day, so they are cached in memory for [DIRECTORY_TTL_MINUTES] to keep cold starts
 * cheap and identical between the UI and the Android Auto browse tree.
 */
class RadioBrowserClient(
    private val hosts: List<String> = DEFAULT_HOSTS,
) {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        // Stations are plain HTTP or HTTPS streams; the app relies on this for playback and does
        // not care about response bodies other than the JSON it asks for.
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val retrofitByHost = HashMap<String, RadioBrowserApi>()
    private var directoryExpiresAt = 0L
    private var cachedCountries: List<Country> = emptyList()
    private var cachedGenres: List<Genre> = emptyList()

    /** Runs [block] against the first mirror that answers, falling through on I/O failures. */
    private suspend fun <T> withFallback(block: suspend (RadioBrowserApi) -> T): T {
        var lastError: IOException? = null
        for (host in hosts) {
            try {
                return block(apiFor(host))
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw lastError ?: IOException("no radio-browser mirror reachable")
    }

    private fun apiFor(host: String): RadioBrowserApi = retrofitByHost.getOrPut(host) {
        Retrofit.Builder()
            .baseUrl(host)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RadioBrowserApi::class.java)
    }

    suspend fun search(
        query: String? = null,
        country: String? = null,
        countryCode: String? = null,
        tag: String? = null,
        language: String? = null,
        centre: Pair<Double, Double>? = null,
        radiusKm: Double? = null,
        order: String? = null,
        limit: Int = 60,
        offset: Int = 0,
    ): List<Station> = withFallback { api ->
        api.searchStations(
            name = query?.trim()?.takeIf { it.isNotEmpty() }?.let(::sanitize),
            country = country?.trim()?.takeIf { it.isNotEmpty() }?.let(::sanitize),
            countryCode = countryCode?.trim()?.takeIf { it.isNotEmpty() }?.uppercase(),
            tag = tag?.trim()?.takeIf { it.isNotEmpty() }?.let(::sanitize),
            language = language?.trim()?.takeIf { it.isNotEmpty() }?.let(::sanitize),
            geoLat = centre?.first,
            geoLong = centre?.second,
            geoDistanceKm = if (centre != null) radiusKm ?: DEFAULT_RADIUS_KM else null,
            order = order,
            // The directory sorts ascending unless `reverse` is set; every ranking we show is
            // "most first", so reverse whenever an order is requested.
            reverse = order != null && order != RadioBrowserApi.ORDER_NAME,
            hideBroken = true,
            limit = limit,
            offset = offset,
        ).mapNotNull { it.toDomain() }
    }

    suspend fun popular(limit: Int = 60): List<Station> =
        search(order = RadioBrowserApi.ORDER_CLICKCOUNT, limit = limit)

    suspend fun topVoted(limit: Int = 60): List<Station> =
        search(order = RadioBrowserApi.ORDER_VOTES, limit = limit)

    suspend fun byCountry(country: String, limit: Int = 100): List<Station> =
        search(country = country, order = RadioBrowserApi.ORDER_VOTES, limit = limit)

    suspend fun byTag(tag: String, limit: Int = 100): List<Station> =
        search(tag = tag, order = RadioBrowserApi.ORDER_VOTES, limit = limit)

    /** `order=random` is the directory's own shuffle, so no local sampling is needed. */
    suspend fun random(countryCode: String? = null): Station? =
        search(countryCode = countryCode, order = RadioBrowserApi.ORDER_RANDOM, limit = 1).firstOrNull()

    /**
     * The directory offers a circle (`geo_lat`/`geo_long`/`geo_distance`) rather than a rectangle,
     * so the globe viewport is reduced to the circle that contains it.
     */
    suspend fun nearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        limit: Int = 300,
    ): List<Station> = search(
        centre = latitude to longitude,
        radiusKm = radiusKm,
        order = RadioBrowserApi.ORDER_VOTES,
        limit = limit,
    )

    suspend fun countries(): List<Country> {
        ensureDirectory()
        return cachedCountries
    }

    suspend fun genres(): List<Genre> {
        ensureDirectory()
        return cachedGenres
    }

    private suspend fun ensureDirectory() {
        val now = System.currentTimeMillis()
        if (cachedCountries.isNotEmpty() && now < directoryExpiresAt) return
        val (countries, tags) = withFallback { api ->
            api.countries() to api.tags(limit = GENRE_LIMIT)
        }
        cachedCountries = countries.mapNotNull { dto ->
            dto.name?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
                Country(name = name, stationCount = dto.stationCount ?: 0)
            }
        }.sortedWith(compareByDescending<Country> { it.stationCount }.thenBy { it.name })

        cachedGenres = tags.mapNotNull { dto ->
            dto.name?.trim()?.takeIf { it.isNotEmpty() }?.let { tag ->
                Genre(
                    label = tag.replace('-', ' ').replaceFirstChar { c -> c.uppercaseChar() },
                    tag = tag,
                    stationCount = dto.stationCount ?: 0,
                )
            }
        }.sortedWith(compareByDescending<Genre> { it.stationCount }.thenBy { it.label })

        directoryExpiresAt = now + TimeUnit.MINUTES.toMillis(DIRECTORY_TTL_MINUTES)
    }

    /**
     * Best-effort click report: the directory asks for it on every playback start. A failure here
     * must never surface to the user, so everything is swallowed.
     */
    suspend fun countClick(stationUuid: String) {
        runCatching { withFallback { api -> api.countClick(stationUuid) } }
    }

    companion object {
        /** Ordered by the operator's own recommendation: the round-robin entry first. */
        val DEFAULT_HOSTS = listOf(
            "https://all.api.radio-browser.info/",
            "https://de1.api.radio-browser.info/",
            "https://fi1.api.radio-browser.info/",
            "https://nl1.api.radio-browser.info/",
        )

        private const val DEFAULT_RADIUS_KM = 1_000.0
        private const val GENRE_LIMIT = 300
        private const val DIRECTORY_TTL_MINUTES = 180L

        /**
         * The API documents that some characters in search terms are rejected or need encoding;
         * trimming the ones that carry no meaning for a station name avoids a 4xx round-trip.
         */
        private val FORBIDDEN = charArrayOf('%', '(', ')')

        private fun sanitize(value: String): String {
            var out = value
            for (c in FORBIDDEN) out = out.replace(c.toString(), " ")
            return out.replace(Regex("\\s+"), " ").trim()
        }
    }
}

internal fun StationDto.toDomain(): Station? {
    val uuid = stationUuid?.trim().orEmpty()
    val stream = urlResolved?.trim().orEmpty().ifEmpty { url?.trim().orEmpty() }
    if (uuid.isEmpty() || stream.isEmpty()) return null
    return Station(
        id = uuid,
        name = name?.trim().orEmpty().ifEmpty { stream },
        streamUrl = stream,
        homepage = homepage?.trim()?.takeIf { it.isNotEmpty() },
        faviconUrl = favicon?.trim()?.takeIf { it.isNotEmpty() },
        country = country?.trim()?.takeIf { it.isNotEmpty() },
        countryCode = countryCode?.trim()?.takeIf { it.isNotEmpty() }?.uppercase(),
        state = state?.trim()?.takeIf { it.isNotEmpty() },
        language = language?.trim()?.takeIf { it.isNotEmpty() },
        tags = tags.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() },
        codec = codec?.trim()?.takeIf { it.isNotEmpty() },
        bitrateKbps = bitrate?.takeIf { it > 0 },
        votes = votes ?: 0,
        clicksLastDay = clickCount ?: 0,
        latitude = geoLat,
        longitude = geoLong,
    )
}
