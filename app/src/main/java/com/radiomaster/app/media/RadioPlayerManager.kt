package com.radiomaster.app.media

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.usecase.AddRecentUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioPlayerManager @Inject constructor(
    private val application: Application,
    private val addRecentUseCase: AddRecentUseCase
) : CoroutineScope {
    override val coroutineContext = Dispatchers.Main

    private val player: ExoPlayer = ExoPlayer.Builder(application.applicationContext).build()

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentStation = MutableStateFlow<Station?>(null)
    val currentStation: StateFlow<Station?> = _currentStation.asStateFlow()

    private var foregroundServiceStarted = false

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _playbackState.value = when (state) {
                    Player.STATE_IDLE -> PlaybackState.IDLE
                    Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                    Player.STATE_READY -> PlaybackState.READY
                    Player.STATE_ENDED -> PlaybackState.IDLE
                    else -> PlaybackState.IDLE
                }
                if (state == Player.STATE_READY) {
                    _currentStation.value?.let { launch { addRecentUseCase(it) } }
                } else if (state == Player.STATE_IDLE && !player.isPlaying) {
                    ensureForegroundService(false)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Timber.e(error, "Błąd odtwarzacza")
                _playbackState.value = PlaybackState.ERROR
            }
        })
    }

    val playerInstance: ExoPlayer get() = player

    fun play(station: Station) {
        _currentStation.value = station
        val mediaItem = MediaItem.fromUri(station.url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        ensureForegroundService(true)
    }

    fun pause() = player.playWhenReady = false
    fun resume() = player.playWhenReady = true
    fun toggle() = player.playWhenReady = !player.playWhenReady
    fun stop() {
        player.stop()
        _currentStation.value = null
        ensureForegroundService(false)
    }

    fun release() {
        stop()
        player.release()
    }

    val isPlaying: Boolean get() = player.isPlaying

    private fun ensureForegroundService(shouldBeRunning: Boolean) {
        val context = application.applicationContext
        val intent = Intent(context, PlayerForegroundService::class.java).apply {
            action = PlayerForegroundService.ACTION_TOGGLE_PLAYBACK
        }
        if (shouldBeRunning && !foregroundServiceStarted) {
            context.startForegroundService(intent)
            foregroundServiceStarted = true
        } else if (!shouldBeRunning && foregroundServiceStarted) {
            context.stopService(intent)
            foregroundServiceStarted = false
        }
    }
}

enum class PlaybackState { IDLE, CONNECTING, BUFFERING, READY, ERROR }
