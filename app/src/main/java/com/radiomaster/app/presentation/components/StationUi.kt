package com.radiomaster.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.radiomaster.app.R
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.media.PlaybackUiState

/**
 * Colours for the station tile. Favicons are remote images that often 404, and a broken image in
 * every row costs more than the logo is worth, so the app draws a deterministic tile instead —
 * the same colours the car sees in the media notification fallback.
 */
private val TilePalette: List<Color> = listOf(
    Color(0xFF1B4C88), Color(0xFF2E6F5E), Color(0xFF8A5A18), Color(0xFF6B3A72),
    Color(0xFF1F5F73), Color(0xFF8C3A3A), Color(0xFF3B5F2A), Color(0xFF4A4E8C),
)

private fun tileColors(station: Station): Pair<Color, Color> {
    val index = (station.id.hashCode() % TilePalette.size + TilePalette.size) % TilePalette.size
    val base = TilePalette[index]
    return base to base.copy(alpha = 0.55f)
}

@Composable
fun StationTile(station: Station, modifier: Modifier = Modifier, size: Int = 44) {
    val (start, end) = tileColors(station)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(if (size >= 72) 20 else 12).dp)
            .background(Brush.linearGradient(listOf(start, end))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = station.name.take(2).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 3).sp,
            maxLines = 1,
        )
    }
}

/**
 * One row of a station list: tile, names, live status, favorite and play controls.
 *
 * [onClick] is "play or pause this station": screens own that decision because the currently
 * playing row must pause instead of restarting, and the app never offers a plain "select".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StationRow(
    station: Station,
    playback: PlaybackUiState,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCurrent = playback.station?.id == station.id
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = if (isCurrent) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StationTile(station = station)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = listOfNotNull(
                    station.locationLabel.takeIf { it.isNotEmpty() },
                    station.bitrateKbps?.let { "$it kbps" },
                ).joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (isCurrent && playback.isBuffering) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    painter = painterResource(
                        if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                    ),
                    contentDescription = stringResource(
                        if (isFavorite) R.string.station_favorite_remove else R.string.station_favorite_add,
                    ),
                    tint = if (isFavorite) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isCurrent) {
                Icon(
                    painter = painterResource(if (playback.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                    contentDescription = stringResource(
                        if (playback.isPlaying) R.string.cd_pause else R.string.cd_play,
                    ),
                    modifier = Modifier.padding(end = 10.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
        action?.invoke()
    }
}

@Composable
private fun LoadingBlock(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MessageBlock(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Wraps a browse list's three non-content states so every screen words them the same way. */
@Composable
fun ListStateBlock(
    isLoading: Boolean,
    failed: Boolean,
    isEmpty: Boolean,
    emptyText: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> LoadingBlock(label = stringResource(R.string.globe_loading), modifier = modifier)
        failed -> Column(
            modifier = modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MessageBlock(text = stringResource(R.string.status_error_generic))
            // A real button, not bare text: the retry has to be a 48 dp target like every other
            // control in the app.
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.common_retry))
            }
        }
        isEmpty -> MessageBlock(text = emptyText, modifier = modifier)
    }
}
