package com.radiomaster.app.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.radiomaster.app.domain.model.Station

/**
 * The single translation between domain objects and Media3 items.
 *
 * Media ids are namespaced (`station:` / `collection:`) so a controller can hand back an id it got
 * from the browse tree and the service always knows which level it refers to.
 */
internal const val STATION_ID_PREFIX = "station:"
internal const val COLLECTION_ID_PREFIX = "collection:"

internal fun stationMediaId(stationId: String): String = STATION_ID_PREFIX + stationId
internal fun collectionMediaId(browseId: String): String = COLLECTION_ID_PREFIX + browseId

/** Accepts both a namespaced id and a raw uuid, because Auto may echo either back. */
internal fun idOfStation(mediaId: String): String = mediaId.removePrefix(STATION_ID_PREFIX)
internal fun idOfCollection(mediaId: String): String = mediaId.removePrefix(COLLECTION_ID_PREFIX)

fun Station.toMediaItem(): MediaItem {
    val mediaId = stationMediaId(id)
    val genreTag = tags.firstOrNull()
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(streamUrl)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name)
                .setDisplayTitle(name)
                // `station` is what Android Auto renders on the "now playing" page for live audio.
                .setStation(name)
                .setArtist(locationLabel.ifEmpty { country.orEmpty() })
                .setSubtitle(genreTag?.replace('-', ' ')?.replaceFirstChar { it.uppercaseChar() })
                .setGenre(genreTag)
                .setArtworkUri(faviconUrl?.toSafeUri())
                .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                .setIsPlayable(true)
                .setIsBrowsable(false)
                .build(),
        )
        .setTag(mediaId)
        .build()
}

/**
 * A directory node of the browse tree: browsable, never playable. Stations in the same list are
 * produced by [Station.toMediaItem], which is browsable=false/playable=true — that split is what
 * lets the car offer "play" straight from a station row without a second round-trip.
 */
internal fun directoryItem(browseId: String, title: String): MediaItem {
    val mediaId = collectionMediaId(browseId)
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setDisplayTitle(title)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_RADIO_STATIONS)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build(),
        )
        .setTag(mediaId)
        .build()
}

/** `Uri.parse` is lenient, but a malformed favicon URL must never take playback down with it. */
private fun String.toSafeUri(): Uri? = runCatching { Uri.parse(this) }.getOrNull()
