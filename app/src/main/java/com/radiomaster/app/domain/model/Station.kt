package com.radiomaster.app.domain.model

/**
 * A radio station, as far as the app cares about it.
 *
 * Free of serialization and framework types on purpose: the Gson DTOs live in `data/remote` and
 * the Room rows in `data/local`, both mapped here at the repository boundary, so the UI, Media3
 * and the database all speak the same language and none of them depends on the API shape.
 */
data class Station(
    val id: String,
    val name: String,
    val streamUrl: String,
    val homepage: String? = null,
    val faviconUrl: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val state: String? = null,
    val language: String? = null,
    val tags: List<String> = emptyList(),
    val codec: String? = null,
    val bitrateKbps: Int? = null,
    val votes: Int = 0,
    val clicksLastDay: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    val hasCoordinates: Boolean
        get() = mapPosition != null

    /** `Poland · Mazovia`-style subtitle under a station name; may be empty. */
    val locationLabel: String
        get() {
            val region = state?.trim()
                ?.removePrefix("/")?.trim()
                ?.takeIf { it.isNotEmpty() && !it.equals("n/a", ignoreCase = true) }
            val countryPart = country?.trim()?.takeIf { it.isNotEmpty() }
            return listOfNotNull(countryPart, region).joinToString(" · ")
        }

    /** Flag for the two-letter ISO code; a globe when the record has none. */
    val regionEmoji: String
        get() {
            val code = countryCode?.trim()?.uppercase()?.takeIf { it.length == 2 && it.all(Char::isLetter) }
                ?: return "\uD83C\uDF10" // 🌐
            val base = REGIONAL_INDICATOR_A - 'A'.code
            return buildString {
                code.forEach { appendCodePoint(base + it.code) }
            }
        }

    /** Equirectangular position for the globe; null when the record has no usable geo data. */
    val mapPosition: MapPosition?
        get() {
            val lat = latitude ?: return null
            val lon = longitude ?: return null
            if (lat.isNaN() || lon.isNaN()) return null
            if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return null
            if (lat == 0.0 && lon == 0.0) return null // "null island": placeholder, not a location
            return MapPosition(
                latitude = lat,
                longitude = lon,
                x = ((lon + 180.0) / 360.0).toFloat(),
                y = ((90.0 - lat) / 180.0).toFloat(),
            )
        }

    private companion object {
        const val REGIONAL_INDICATOR_A = 0x1F1E6
    }
}

/** Position on an equirectangular map: (0,0) is top-left, (1,1) bottom-right. */
data class MapPosition(
    val latitude: Double,
    val longitude: Double,
    val x: Float,
    val y: Float,
)

/** A selectable genre, backed by the `tag` filter of the station catalogue. */
data class Genre(val label: String, val tag: String, val stationCount: Int = 0)

/** A country of the catalogue; radio-browser filters by full name. */
data class Country(val name: String, val stationCount: Int = 0)
