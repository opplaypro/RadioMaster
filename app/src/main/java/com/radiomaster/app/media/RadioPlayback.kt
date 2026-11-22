package com.radiomaster.app.media

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.LocalRepository
import com.radiomaster.app.domain.repository.StationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Playback snapshot the phone UI renders; the car UI reads the same state through the session. */
data class PlaybackUiState(
    val station: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasError: Boolean = false,
) {
    val isIdle: Boolean get() = station == null
}

/**
 * Owns the [ExoPlayer] instance for the whole process.
 *
 * Both entry points share it: the phone screens call [play] and [togglePlayPause] directly, while
 * [RadioMediaSessionService] wraps the very same player in a `MediaLibrarySession`, which is what
 * gives the notification, the lock screen and Android Auto one source of truth instead of three
 * that have to be kept in sync. The player is built in
 * [com.radiomaster.app.RadioMasterApplication.onCreate] because ExoPlayer binds to the looper of
 * the creating thread — so nothing here may be reached from a worker thread.
 */
@OptIn(UnstableApi::class)
class RadioPlayback(
    application: Application,
    private val localRepository: LocalRepository,
    private val stationRepository: StationRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) = publish()

        override fun onIsPlayingChanged(isPlaying: Boolean) = publish()

        override fun onPlayerError(error: PlaybackException) {
            // The message stays out of the UI: PlaybackException text is English, developer-facing
            // and localized nowhere, so the screen shows the generic stream-error copy instead.
            _state.value = _state.value.copy(isPlaying = false, isBuffering = false, hasError = true)
        }
    }

    val player: ExoPlayer = ExoPlayer.Builder(application)
        // USAGE_MEDIA plus audio focus: a call or a navigation prompt ducks or pauses us, and a
        // permanent loss stops playback instead of fighting the other app for the stream.
        .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
        // Becoming noisy (headset unplugged, Bluetooth dropped) must pause, not blast the phone.
        .setHandleAudioBecomingNoisy(true)
        // Live streams buffer for seconds at a time; without a wake lock Doze can stall them.
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()
        .also { it.addListener(playerListener) }

    private fun publish() {
        _state.value = _state.value.copy(
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            hasError = player.playerError != null,
        )
    }

    /** Starts the station, or keeps it running when it is already the current item. */
    fun play(station: Station) {
        if (player.currentMediaItem?.mediaId == stationMediaId(station.id)) {
            player.playWhenReady = true
            publish()
            return
        }
        _state.value = _state.value.copy(station = station, hasError = false, isBuffering = true)
        player.setMediaItem(station.toMediaItem())
        player.prepare()
        player.playWhenReady = true
        scope.launch {
            // History is what the car's first two browse nodes show, so it has to be written even
            // when the phone started playback. The click report is what keeps the public ranking
            // honest. Neither may delay the first audio frame, and neither may fail the play call.
            runCatching { localRepository.recordPlay(station) }
            runCatching { stationRepository.reportPlay(station.id) }
        }
    }

    fun togglePlayPause() {
        if (player.currentMediaItem == null) return
        player.playWhenReady = !player.playWhenReady
        publish()
    }

    /** Re-prepares the current station; the retry button in the bar and the error states use it. */
    fun retry() {
        val station = _state.value.station ?: return
        _state.value = _state.value.copy(hasError = false, isBuffering = true)
        player.setMediaItem(station.toMediaItem())
        player.prepare()
        player.playWhenReady = true
    }

    fun stop() {
        player.stop()
        _state.value = PlaybackUiState()
    }
}
