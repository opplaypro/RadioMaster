package com.radiomaster.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiomaster.app.R
import com.radiomaster.app.domain.model.BrowseIds
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.presentation.components.ListStateBlock
import com.radiomaster.app.presentation.components.SectionTitle
import com.radiomaster.app.presentation.components.StationTile
import com.radiomaster.app.presentation.viewmodel.BrowseViewModel
import com.radiomaster.app.presentation.viewmodel.LibraryViewModel
import com.radiomaster.app.presentation.viewmodel.PlaybackViewModel

/**
 * The browse tab: two horizontal rails of live stations and two tag clouds of directory entries.
 *
 * Everything on the page comes from [BrowseViewModel.loadHome], which fires its four requests
 * concurrently. Categories are opened by browse id rather than by a dedicated query, which is the
 * same id the car's sidebar uses — so "Poland" on the phone and "Poland" in the car are the same
 * request against the same code path.
 */
@Composable
fun HomeScreen(
    browseViewModel: BrowseViewModel,
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel,
    onOpenCategory: (String) -> Unit,
) {
    LaunchedEffect(Unit) { browseViewModel.loadHome() }
    val state by browseViewModel.state.collectAsStateWithLifecycle()
    val playbackState by playbackViewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp)) {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.globe_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.isLoading && state.popular.isEmpty()) {
            item {
                ListStateBlock(
                    isLoading = true,
                    failed = false,
                    isEmpty = false,
                    emptyText = "",
                    onRetry = { },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.failed && state.popular.isEmpty()) {
            item {
                ListStateBlock(
                    isLoading = false,
                    failed = true,
                    isEmpty = false,
                    emptyText = "",
                    onRetry = { browseViewModel.retryHome() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        stationRail(
            titleRes = R.string.home_popular,
            stations = state.popular,
            currentId = playbackState.station?.id,
            onStationClick = { station -> play(station, playbackState, playbackViewModel) },
        )
        stationRail(
            titleRes = R.string.home_top_voted,
            stations = state.topVoted,
            currentId = playbackState.station?.id,
            onStationClick = { station -> play(station, playbackState, playbackViewModel) },
        )

        item {
            SectionTitle(text = stringResource(R.string.home_countries))
            DirectoryRow(
                entries = state.countries.map { it.name to it.stationCount },
                onClick = { country -> onOpenCategory(BrowseIds.country(country)) },
            )
        }

        item {
            SectionTitle(text = stringResource(R.string.home_genres))
            DirectoryRow(
                entries = state.genres.map { it.label to it.stationCount },
                onClick = { genre -> onOpenCategory(BrowseIds.genre(genre.lowercase())) },
            )
        }
    }
}

private fun LazyListScope.stationRail(
    titleRes: Int,
    stations: List<Station>,
    currentId: String?,
    onStationClick: (Station) -> Unit,
) {
    if (stations.isEmpty()) return
    item { SectionTitle(text = stringResource(titleRes)) }
    item {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items = stations, key = { station -> station.id }) { station ->
                StationCard(
                    station = station,
                    isCurrent = station.id == currentId,
                    onClick = { onStationClick(station) },
                )
            }
        }
    }
}

/** A rail card taps to play; tapping the station that is already playing pauses it. */
private fun play(
    station: Station,
    playbackState: com.radiomaster.app.media.PlaybackUiState,
    playbackViewModel: PlaybackViewModel,
) {
    if (playbackState.station?.id == station.id) playbackViewModel.toggle() else playbackViewModel.play(station)
}

@Composable
@Composable
private fun StationCard(station: Station, isCurrent: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            StationTile(station = station, size = 56)

            Spacer(Modifier.height(10.dp))
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val subtitle = station.locationLabel.ifEmpty { station.codec.orEmpty() }
            if (subtitle.isNotEmpty() || isCurrent) {
                Text(
                    text = if (isCurrent) stringResource(R.string.status_playing) else subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Horizontally scrolling pills of directory entries with their station counts. */
@Composable
private fun DirectoryRow(entries: List<Pair<String, Int>>, onClick: (String) -> Unit) {
    if (entries.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = entries, key = { it.first }) { (label, count) ->
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            ) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = { onClick(label) })
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = label, style = MaterialTheme.typography.labelLarge)
                    if (count > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
