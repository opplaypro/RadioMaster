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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiomaster.app.R
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.presentation.components.ListStateBlock
import com.radiomaster.app.presentation.components.StationDetailDialog
import com.radiomaster.app.presentation.components.StationRow
import com.radiomaster.app.presentation.viewmodel.BrowseViewModel
import com.radiomaster.app.presentation.viewmodel.LibraryViewModel
import com.radiomaster.app.presentation.viewmodel.PlaybackViewModel
import kotlinx.coroutines.delay

/** Which field a typed query is matched against. */
private enum class SearchField { NAME, TAG, COUNTRY }

/**
 * Search tab.
 *
 * The directory has no free-text endpoint: one query is matched against `name`, `tag` or `country`
 * depending on the selected field, so the user picks the field instead of the app guessing. The
 * search restarts on a pause in typing rather than per keystroke — each request hits a shared
 * community mirror, so a keystroke-per-request policy would be rude as well as slow.
 */
@Composable
fun SearchScreen(
    browseViewModel: BrowseViewModel,
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var details by remember { mutableStateOf<Station?>(null) }
    var field by remember { mutableStateOf(SearchField.NAME) }
    val state by browseViewModel.state.collectAsStateWithLifecycle()
    val playback by playbackViewModel.state.collectAsStateWithLifecycle()
    val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())
    val favoriteIds = remember(favorites) { favorites.map(Station::id).toSet() }

    // One place maps the selected field onto the directory parameter, so the debounced search and
    // the retry button cannot drift apart.
    fun submit(text: String, target: SearchField) {
        val trimmed = text.trim()
        when (target) {
            SearchField.NAME -> browseViewModel.search(query = trimmed)
            SearchField.TAG -> browseViewModel.search(query = "", genre = trimmed)
            SearchField.COUNTRY -> browseViewModel.search(query = "", country = trimmed)
        }
    }

    LaunchedEffect(query, field) {
        delay(DEBOUNCE_MILLIS)
        submit(query, field)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.search_title)) },
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = {
                    Icon(painter = painterResource(R.drawable.ic_search), contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.search_clear),
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { /* the debounce above already fired */ }),
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldToggle(label = stringResource(R.string.search_field_name), selected = field == SearchField.NAME) {
                    field = SearchField.NAME
                }
                FieldToggle(label = stringResource(R.string.search_field_tag), selected = field == SearchField.TAG) {
                    field = SearchField.TAG
                }
                FieldToggle(
                    label = stringResource(R.string.search_field_country),
                    selected = field == SearchField.COUNTRY,
                ) { field = SearchField.COUNTRY }
                Spacer(Modifier.weight(1f))
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
            if (query.isNotBlank() && state.stations.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.search_results, state.stations.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        when {
            query.isBlank() -> ListStateBlock(
                isLoading = false,
                failed = false,
                isEmpty = true,
                emptyText = stringResource(R.string.search_empty),
                onRetry = { },
                modifier = Modifier.fillMaxWidth(),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 6.dp, bottom = 20.dp),
            ) {
                item {
                    ListStateBlock(
                        isLoading = state.isLoading && state.stations.isEmpty(),
                        failed = state.failed,
                        isEmpty = !state.isLoading && state.stations.isEmpty(),
                        emptyText = stringResource(R.string.search_no_results),
                        onRetry = { submit(query, field) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                items(items = state.stations, key = { station -> station.id }) { station ->
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
    }
}

@Composable
private fun FieldToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.height(32.dp),
    ) {
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
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
