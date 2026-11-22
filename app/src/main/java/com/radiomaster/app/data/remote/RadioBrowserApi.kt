package com.radiomaster.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints of radio-browser.info that the app uses.
 *
 * Every response field is nullable with a default, because the database is community-edited and a
 * missing `homepage` / `favicon` / geo pair is documented, not exceptional. Numbers (`bitrate`,
 * `votes`, `clickcount`) are typed as numbers: Gson coerces the numeric-string form that some
 * mirrors still emit, so both shapes parse.
 */
data class StationDto(
    @SerializedName("stationuuid") val stationUuid: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("url_resolved") val urlResolved: String? = null,
    @SerializedName("homepage") val homepage: String? = null,
    @SerializedName("favicon") val favicon: String? = null,
    @SerializedName("tags") val tags: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("countrycode") val countryCode: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("language") val language: String? = null,
    @SerializedName("codec") val codec: String? = null,
    @SerializedName("bitrate") val bitrate: Int? = null,
    @SerializedName("votes") val votes: Int? = null,
    @SerializedName("clickcount") val clickCount: Int? = null,
    @SerializedName("geo_lat") val geoLat: Double? = null,
    @SerializedName("geo_long") val geoLong: Double? = null,
    @SerializedName("lastcheckok") val lastCheckOk: Int? = null,
)

/** `/json/countries` and `/json/languages` entries. */
data class NamedCountDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("stationcount") val stationCount: Int? = null,
)

/** `/json/tags` entry; `name` is the tag to filter `stations/search` with. */
data class TagDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("stationcount") val stationCount: Int? = null,
)

/** Envelope of `/json/url/{stationuuid}`. */
data class ClickDto(
    @SerializedName("ok") val ok: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("stationuuid") val stationUuid: String? = null,
)

/**
 * A plain Retrofit interface over the community radio directory. There is no API key and no
 * authentication; the only write-ish endpoint is [RadioBrowserApi.countClick], which the
 * directory asks every client to call when playback starts.
 */
interface RadioBrowserApi {
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String? = null,
        @Query("country") country: String? = null,
        @Query("countrycode") countryCode: String? = null,
        @Query("tag") tag: String? = null,
        @Query("language") language: String? = null,
        @Query("geo_lat") geoLat: Double? = null,
        @Query("geo_long") geoLong: Double? = null,
        @Query("geo_distance") geoDistanceKm: Double? = null,
        @Query("order") order: String? = null,
        @Query("reverse") reverse: Boolean? = null,
        @Query("hidebroken") hideBroken: Boolean? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
    ): List<StationDto>

    // The list endpoints have no guaranteed ordering, so the client sorts them by station count
    // instead of relying on an `order` value.
    @GET("json/countries")
    suspend fun countries(@Query("hidebroken") hideBroken: Boolean = true): List<NamedCountDto>

    @GET("json/tags")
    suspend fun tags(
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("limit") limit: Int? = null,
    ): List<TagDto>

    @GET("json/languages")
    suspend fun languages(@Query("hidebroken") hideBroken: Boolean = true): List<NamedCountDto>

    /** Increases the click count once per day and IP, and returns the current stream URL. */
    @GET("json/url/{stationUuid}")
    suspend fun countClick(@Path("stationUuid") stationUuid: String): ClickDto

    companion object {
        const val ORDER_STATION_COUNT = "stationcount"
        const val ORDER_CLICKCOUNT = "clickcount"
        const val ORDER_VOTES = "votes"
        const val ORDER_NAME = "name"
        const val ORDER_RANDOM = "random"
    }
}
