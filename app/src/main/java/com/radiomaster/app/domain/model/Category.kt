package com.radiomaster.app.domain.model

/**
 * Identifiers of the browseable collections.
 *
 * The same ids address a section in the phone UI (home rails, category pages) and a node of the
 * Android Auto media browse tree, where they are wrapped in a `collection:` media id. Keeping one
 * string vocabulary means "Poland" on the phone and "Poland" in the car are the same request
 * against the same code path, and a page restored in the car resolves to a real list.
 *
 * The ids are plain strings because they travel through navigation routes and through IPC to the
 * controller; an enum would have to be mapped on both sides for no benefit.
 */
object BrowseIds {
    const val ROOT: String = "root"
    const val FAVORITES: String = "favorites"
    const val HISTORY: String = "history"
    const val POPULAR: String = "popular"
    const val TOP_VOTED: String = "top_voted"
    const val RANDOM: String = "random"
    const val COUNTRIES: String = "countries"
    const val GENRES: String = "genres"

    private const val COUNTRY_PREFIX = "country/"
    private const val GENRE_PREFIX = "genre/"

    /** Children of the root node, in the order the car's sidebar and the home screen show them. */
    val rootChildren: List<String> = listOf(
        FAVORITES,
        HISTORY,
        POPULAR,
        TOP_VOTED,
        COUNTRIES,
        GENRES,
        RANDOM,
    )

    /** Collections answered from the device, so they never need a network round-trip. */
    val localCollections: Set<String> = setOf(FAVORITES, HISTORY)

    fun country(countryName: String): String = COUNTRY_PREFIX + countryName
    fun genre(tag: String): String = GENRE_PREFIX + tag

    fun countryOf(id: String): String? = id.removePrefix(COUNTRY_PREFIX).takeIf { id.startsWith(COUNTRY_PREFIX) }
    fun genreOf(id: String): String? = id.removePrefix(GENRE_PREFIX).takeIf { id.startsWith(GENRE_PREFIX) }

    /** True for nodes that expand into more nodes instead of into stations. */
    fun isDirectory(id: String): Boolean = id == ROOT || id == COUNTRIES || id == GENRES
}

/**
 * A directory entry of the tree, used by the phone UI when it needs a label and a target id
 * together. The car gets the same pair through `MediaMetadata` on a browsable item.
 */
data class BrowseCategory(
    val id: String,
    val title: String,
    val stationCount: Int = 0,
)
