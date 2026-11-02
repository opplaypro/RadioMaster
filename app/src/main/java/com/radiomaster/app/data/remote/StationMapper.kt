package com.radiomaster.app.data.remote

import com.radiomaster.app.domain.model.Station

fun StationDto.toDomain(isFavorite: Boolean = false): Station {
    val streamUrl = urlResolved?.takeIf { it.isNotEmpty() } ?: url
    return Station(
        id = id ?: "",
        name = name ?: "Nieznana stacja",
        url = streamUrl ?: "",
        homepage = homepage,
        faviconUrl = favicon,
        country = country,
        countryCode = countryCode,
        state = state,
        language = language,
        tags = tags,
        codec = codec,
        bitrate = bitrate?.toIntOrNull(),
        votes = votes?.toIntOrNull(),
        rating = rating?.toFloatOrNull(),
        clicktrust = clicktrust,
        ssl = ssl?.let { it == 1 },
        lastCheckTime = lastCheckTime,
        isFavorite = isFavorite
    )
}
