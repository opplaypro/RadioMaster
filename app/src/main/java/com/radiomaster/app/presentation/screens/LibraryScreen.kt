package com.radiomaster.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiomaster.app.R
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.presentation.components.ListStateBlock
import com.radiomaster.app.presentation.components.StationDetailDialog
import com.radiomaster.app.presentation.components.StationRow
import com.radiomaster.app.presentation.viewmodel.LibraryViewModel
import com.radiomaster.app.presentation.viewmodel.PlaybackViewModel

/**
 * Favorites and history in one place.
 *
 * Both lists are Room flows, so starring a station anywhere else in the app (or in the car) is
 * reflected here without any refresh action. They are also exactly the two collections the Android
 * Auto sidebar shows first, which is why the screen has no search of its own.
 */
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
) {
    var showHistory by rememberSaveable { mutableStateOf(false) }
    val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val history by libraryViewModel.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val playback by playbackViewModel.state.collectAsStateWithLifecycle()
    val favoriteIds = remember(favorites) { favorites.map(Station::id).toSet() }

    val stations = if (showHistory) history else favorites
    var details by remember { mutableStateOf<Station?>(null) }
    val emptyText = stringResource(
        if (showHistory) R.string.library_recents_empty else R.string.library_favorites_empty,
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp)) {
            Text(
                text = stringResource(R.string.library_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Segment(
                    label = stringResource(R.string.library_favorites),
                    selected = !showHistory,
                    onClick = { showHistory = false },
                )
                Segment(
                    label = stringResource(R.string.library_recents),
                    selected = showHistory,
                    onClick = { showHistory = true },
                )
                Spacer(Modifier.weight(1f))
                if (showHistory && history.isNotEmpty()) {
                    TextButton(onClick = { libraryViewModel.clearHistory() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text(text = stringResource(R.string.library_clear_recents))
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
        ) {
            item {
                ListStateBlock(
                    isLoading = false,
                    failed = false,
                    isEmpty = stations.isEmpty(),
                    emptyText = emptyText,
                    onRetry = { },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            items(items = stations, key = { station -> station.id }) { station ->
                StationRow(
                    station = station,
                    playback = playback,
                    isFavorite = station.id in favoriteIds,
                    onClick = {
                        if (playback.station?.id == station.id) playbackViewModel.toggle()
                        else playbackViewModel.play(station)
                    },
                    onToggleFavorite = { libraryViewModel.toggleFavorite(station) },
                    onLongClick = { details = station },
                )
            }
            if (stations.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.library_offline_note),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
                }
            }
        }
    }

    details?.let { station ->
        StationDetailDialog(
            station = station,
            isFavorite = station.id in favoriteIds,
            isPlaying = playback.station?.id == station.id && playback.isPlaying,
            onDismiss = { details = null },
            onPlayPause = {
                if (playback.station?.id == station.id) playbackViewModel.toggle()
                else playbackViewModel.play(station)
            },
            onToggleFavorite = { libraryViewModel.toggleFavorite(station) },
        )
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
