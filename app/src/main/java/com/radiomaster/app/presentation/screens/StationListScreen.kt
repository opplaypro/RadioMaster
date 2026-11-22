package com.radiomaster.app.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiomaster.app.R
import com.radiomaster.app.domain.model.BrowseIds
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.presentation.components.ListStateBlock
import com.radiomaster.app.presentation.components.StationDetailDialog
import com.radiomaster.app.presentation.components.StationRow
import com.radiomaster.app.presentation.viewmodel.BrowseViewModel
import com.radiomaster.app.presentation.viewmodel.LibraryViewModel
import com.radiomaster.app.presentation.viewmodel.PlaybackViewModel

/**
 * One page per browse id — a country, a genre, favorites or history.
 *
 * The page does not know what it is showing: it asks [BrowseViewModel.open] with the id, and the
 * same id resolves to the same list in the car's browse tree. Country and genre names therefore
 * come from the data itself and only the fixed collections need a string resource.
 */
@Composable
fun StationListScreen(
    browseId: String,
    browseViewModel: BrowseViewModel,
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel,
    onBack: () -> Unit,
) {
    // Favorites and history are local, so they are observed instead of fetched: toggling a star on
    // this page has to update the list without a manual refresh. Everything else is a remote page and
    // is asked for once per browse id.
    val isLocalCollection = browseId == BrowseIds.FAVORITES || browseId == BrowseIds.HISTORY
    LaunchedEffect(browseId, isLocalCollection) {
        if (!isLocalCollection) browseViewModel.open(browseId)
    }
    val state by browseViewModel.state.collectAsStateWithLifecycle()
    val localSource = if (browseId == BrowseIds.FAVORITES) libraryViewModel.favorites else libraryViewModel.history
    val localStations by localSource.collectAsStateWithLifecycle(initialValue = emptyList())
    val playback by playbackViewModel.state.collectAsStateWithLifecycle()
    val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteIds = remember(favorites) { favorites.map(Station::id).toSet() }
    val stations = if (isLocalCollection) localStations else state.stations
    var details by remember { mutableStateOf<Station?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = browseTitle(browseId), onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
        ) {
            item {
                ListStateBlock(
                    isLoading = state.isLoading && !isLocalCollection,
                    failed = state.failed && !isLocalCollection,
                    isEmpty = stations.isEmpty(),
                    emptyText = emptyTextFor(browseId),
                    onRetry = { browseViewModel.refresh() },
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

/**
 * Title row of a pushed page.
 *
 * The tabs above it belong to the Scaffold, so a pushed page brings its own back affordance
 * instead of relying on the system gesture alone — in a car or with an accessibility service the
 * gesture is not always reachable.
 */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.common_back),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun browseTitle(browseId: String): String {
    BrowseIds.countryOf(browseId)?.let { return it }
    BrowseIds.genreOf(browseId)?.let { tag ->
        return tag.replace('-', ' ').replaceFirstChar { char -> char.uppercaseChar() }
    }
    val res = when (browseId) {
        BrowseIds.ROOT -> R.string.home_title
        BrowseIds.FAVORITES -> R.string.library_favorites
        BrowseIds.HISTORY -> R.string.library_recents
        BrowseIds.POPULAR -> R.string.home_popular
        BrowseIds.TOP_VOTED -> R.string.home_top_voted
        BrowseIds.COUNTRIES -> R.string.home_countries
        BrowseIds.GENRES -> R.string.home_genres
        BrowseIds.RANDOM -> R.string.globe_shuffle
        else -> R.string.app_name
    }
    return stringResource(res)
}

@Composable
private fun emptyTextFor(browseId: String): String = when (browseId) {
    BrowseIds.FAVORITES -> stringResource(R.string.library_favorites_empty)
    BrowseIds.HISTORY -> stringResource(R.string.library_recents_empty)
    else -> stringResource(R.string.search_no_results)
}
