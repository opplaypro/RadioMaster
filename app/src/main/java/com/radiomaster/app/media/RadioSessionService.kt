package com.radiomaster.app.media

import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionResult
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.StationRepository
import com.radiomaster.app.domain.usecase.SearchStationsUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first

@AndroidEntryPoint
class RadioSessionService : MediaSessionService() {

    @Inject lateinit var playerManager: RadioPlayerManager
    @Inject lateinit var stationRepository: StationRepository
    @Inject lateinit var searchUseCase: SearchStationsUseCase

    private lateinit var mediaLibrarySession: MediaLibrarySession

    override fun onCreate() {
        super.onCreate()

        val callback = object : MediaLibrarySession.Callback() {
            override fun onGetLibraryRoot(
                sessionId: String,
                params: MediaLibrarySession.MediaLibraryParams
            ): SessionResult<List<MediaItem>> {
                return SessionResult.success(listOf(
                    mediaItem("search", getString(R.string.action_search)),
                    mediaItem("popular", getString(R.string.home_featured)),
                    mediaItem("favorites", getString(R.string.favorites_title))
                ))
            }

            override fun onGetChildren(
                sessionId: String,
                parentMediaId: String,
                params: MediaLibrarySession.MediaLibraryParams
            ): SessionResult<List<MediaItem>> {
                return when (parentMediaId) {
                    "popular" -> SessionResult.success(stationRepository.getPopularStations(50).first().map { stationToMediaItem(it) })
                    "favorites" -> SessionResult.success(stationRepository.getFavorites().first().map { stationToMediaItem(it) })
                    else -> SessionResult.success(emptyList())
                }
            }

            override fun onGetSearchResult(
                sessionId: String,
                query: String,
                params: MediaLibrarySession.MediaLibraryParams
            ): SessionResult<List<MediaItem>> {
                return SessionResult.success(searchUseCase.searchFlow(query).first().map { stationToMediaItem(it) })
            }

            override fun onPlayFromSearch(sessionId: String, query: String, extras: Bundle?) {
                val station = searchUseCase.searchFlow(query).first().firstOrNull()
                station?.let { playerManager.play(it) }
            }

            override fun onPlayFromMediaId(sessionId: String, mediaId: String, extras: Bundle?) {
                val station = if (mediaId == "search") null else {
                    stationRepository.getPopularStations(100).first().firstOrNull { it.id == mediaId }
                        ?: stationRepository.getFavorites().first().firstOrNull { it.id == mediaId }
                }
                station?.let { playerManager.play(it) }
            }

            override fun onPlayFromUri(sessionId: String, uri: android.net.Uri, extras: Bundle?) {
                val station = stationRepository.getPopularStations(200).first().firstOrNull { it.url == uri.toString() }
                    ?: stationRepository.getFavorites().first().firstOrNull { it.url == uri.toString() }
                station?.let { playerManager.play(it) }
            }
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, callback)
            .setPlayer(playerManager.playerInstance)
            .build()
    }

    override fun onGetSession(controller: MediaSession.Controller): MediaSession? {
        return mediaLibrarySession.session
    }

    private fun mediaItem(id: String, title: String): MediaItem =
        MediaItem.Builder().setMediaId(id).setTitle(title).build()

    private fun stationToMediaItem(station: Station): MediaItem {
        return MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(android.net.Uri.parse(station.url))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, station.name)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, station.country ?: "")
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, station.language ?: "")
                    .setArtUri(station.faviconUrl?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()
    }
}
