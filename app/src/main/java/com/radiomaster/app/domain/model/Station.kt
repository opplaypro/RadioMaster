package com.radiomaster.app.domain.model

/**
 * Model domenowy stacji radiowej. Niezależny od źródła danych (API, baza lokalna).
 */
data class Station(
    val id: String,
    val name: String,
    val url: String,
    val homepage: String? = null,
    val faviconUrl: String? = null,
    val country: String? = null,
    val countryCode: String? = null,
    val state: String? = null,
    val language: String? = null,
    val tags: String? = null,
    val codec: String? = null,
    val bitrate: Int? = null,
    val votes: Int? = null,
    val rating: Float? = null,
    val clicktrust: String? = null,
    val ssl: Boolean? = null,
    val lastCheckTime: Long? = null,
    /** Czy stacja znajduje się w ulubionych (uzupełniane z bazy lokalnej). */
    val isFavorite: Boolean = false,
    /** Czy stacja była ostatnio odtwarzana. */
    val isRecent: Boolean = false,
    val playedAt: Long? = null
) {
    fun displayBitrate: String = bitrate?.let { "$it kbit/s" } ?: "—"
    fun displayCountry: String = country ?: "Nieznany kraj"
    fun displayLanguage: String = language ?: "—"
    fun displayTags: List<String> = tags?.split(",".toRegex()).map { it.trim() }.filter { it.isNotEmpty() } ?: emptyList()
}
