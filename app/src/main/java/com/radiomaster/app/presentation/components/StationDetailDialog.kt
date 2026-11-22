package com.radiomaster.app.presentation.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.radiomaster.app.R
import com.radiomaster.app.domain.model.Station

/**
 * Everything the app knows about one station, in a dialog.
 *
 * Reached by long-pressing a row: a single tap plays, which is the action that matters while the
 * phone is in a pocket or a holder. Homepage and share are the two things a stream URL cannot
 * replace, so they are here rather than on the row; both degrade to a message when a record has no
 * homepage, which the directory permits.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StationDetailDialog(
    station: Station,
    isFavorite: Boolean,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
            ) {
                Text(
                    text = stringResource(R.string.station_details),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StationTile(station = station, size = 52)
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = station.name, style = MaterialTheme.typography.titleLarge)
                        if (station.locationLabel.isNotEmpty()) {
                            Text(
                                text = station.locationLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                DetailLine(label = stringResource(R.string.station_country), value = station.country ?: "—")
                DetailLine(label = stringResource(R.string.station_region), value = station.state ?: "—")
                DetailLine(label = stringResource(R.string.station_language), value = station.language ?: "—")
                DetailLine(
                    label = stringResource(R.string.station_bitrate),
                    value = station.bitrateKbps?.let { stringResource(R.string.station_bitrate_value, it) }
                        ?: (station.codec ?: "—"),
                )
                DetailLine(label = stringResource(R.string.station_codec), value = station.codec ?: "—")
                DetailLine(label = stringResource(R.string.station_votes), value = station.votes.toString())

                if (station.tags.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.station_tags),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        station.tags.forEach { tag -> TagPill(tag) }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPlayPause, modifier = Modifier.weight(1f)) {
                        Icon(
                            painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(
                                if (isPlaying) R.string.station_pause else R.string.station_play,
                            ),
                        )
                    }
                    TextButton(onClick = onToggleFavorite) {
                        Icon(
                            painter = painterResource(
                                if (isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border,
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(
                                if (isFavorite) R.string.station_favorite_remove else R.string.station_favorite_add,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val homepage = station.homepage
                    OutlinedButton(
                        onClick = { homepage?.let { context.openLink(it) } },
                        enabled = homepage != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(
                                if (homepage != null) R.string.station_homepage else R.string.station_no_homepage,
                            ),
                        )
                    }
                    TextButton(onClick = { context.share(station) }) {
                        Text(text = stringResource(R.string.station_share))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun TagPill(tag: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private fun Context.openLink(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No browser on this device profile: the row stays inert rather than crashing.
    }
}

private fun Context.share(station: Station) {
    val text = buildString {
        append(station.name)
        append("\n")
        append(station.streamUrl)
        station.homepage?.let { append("\n").append(it) }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(Intent.createChooser(intent, station.name)) }
}
