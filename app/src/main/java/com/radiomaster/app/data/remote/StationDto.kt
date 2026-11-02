package com.radiomaster.app.data.remote

import com.google.gson.annotations.SerializedName

data class StationDto(
    @SerializedName("stationuuid") val id: String? = null,
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
    @SerializedName("bitrate") val bitrate: String? = null,
    @SerializedName("votes") val votes: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("clicktrust") val clicktrust: String? = null,
    @SerializedName("ssl") val ssl: Int? = null,
    @SerializedName("lastchecktime") val lastCheckTime: Long? = null
)
