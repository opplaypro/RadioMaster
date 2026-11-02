package com.radiomaster.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface RadioBrowserApi {
    @GET("json/stations/top")
    suspend fun getTopStations(@Query("limit") limit: Int = 100): List<StationDto>

    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String?,
        @Query("limit") limit: Int = 30
    ): List<StationDto>

    @GET("json/stations/bycountry")
    suspend fun stationsByCountry(
        @Query("country") country: String,
        @Query("limit") limit: Int = 50
    ): List<StationDto>

    @GET("json/stations/bylanguage")
    suspend fun stationsByLanguage(
        @Query("language") language: String,
        @Query("limit") limit: Int = 50
    ): List<StationDto>

    @GET("json/stations/bytag")
    suspend fun stationsByTag(
        @Query("tag") tag: String,
        @Query("limit") limit: Int = 50
    ): List<StationDto>

    @GET("json/stations/id")
    suspend fun stationById(@Query("stationuuid") uuid: String): StationDto?
}
