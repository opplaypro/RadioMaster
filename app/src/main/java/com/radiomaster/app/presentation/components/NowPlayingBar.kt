package com.radiomaster.app.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.radiomaster.app.R
import com.radiomaster.app.media.PlaybackUiState

/**
 * The persistent transport bar.
 *
 * It is a thin mirror of the session state rather than a second source of truth: every action goes
 * through [com.radiomaster.app.media.RadioPlayback], which is also what the notification, the lock
 * screen and Android Auto drive, so the four can never disagree.
 */
@Composable
fun NowPlayingBar(
    state: PlaybackUiState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val station = state.station ?: return
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationTile(station = station, size = 36)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(statusLabel(state)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (state.hasError) {
                IconButton(onClick = onRetry) {
                    Icon(
                        painter = painterResource(R.drawable.ic_refresh),
                        contentDescription = stringResource(R.string.status_retry),
                    )
                }
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    painter = painterResource(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = stringResource(
                        if (state.isPlaying) R.string.cd_pause else R.string.cd_play,
                    ),
                    modifier = Modifier.size(28.dp),
                )
            }
            IconButton(onClick = onStop) {
                Icon(
                    painter = painterResource(R.drawable.ic_stop),
                    contentDescription = stringResource(R.string.station_stop),
                )
            }
        }
    }
}

@Composable
private fun statusLabel(state: PlaybackUiState): Int = when {
    state.hasError -> R.string.status_error_generic
    state.isBuffering -> R.string.status_buffering
    state.isPlaying -> R.string.status_playing
    else -> R.string.status_paused
}
