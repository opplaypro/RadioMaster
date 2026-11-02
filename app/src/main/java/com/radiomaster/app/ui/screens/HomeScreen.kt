package com.radiomaster.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.map
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.radiomaster.app.R
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.media.PlaybackState
import com.radiomaster.app.ui.component.CategoryChip
import com.radiomaster.app.ui.component.StationCard
import com.radiomaster.app.ui.navigation.Routes
import com.radiomaster.app.ui.theme.RadioMasterTheme
import com.radiomaster.app.ui.viewmodel.HomeViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyHorizontalGrid
import androidx.compose.foundation.grid.GridCells
import androidx.compose.foundation.lazy.grid.items

@Composable
fun HomeScreen(
    navController: NavHostController,
    onStationSelected: (Station) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val popular by viewModel.popularStations.collectAsStateWithLifecycle(emptyList())
    val recent by viewModel.recentStations.collectAsStateWithLifecycle(emptyList())
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle(emptySet())
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentStationId by viewModel.currentStation.map { it?.id }.collectAsStateWithLifecycle(null)

    val categories = listOf(
        "Polskie", "Rock", "Pop", "Jazz", "Muzyka klasyczna", "Wiadomości", "Sport", "Relaks"
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = stringResource(R.string.home_header_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = stringResource(R.string.home_header_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Polecane (poziomo)
            Text(
                text = stringResource(R.string.home_featured),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(1),
            modifier = Modifier.height(160.dp)
        ) {
            items(popular.take(8)) { station ->
                StationCard(
                    station = station,
                    isFavorite = station.id in favoriteIds,
                    onClick = {
                        viewModel.playStation(station)
                        onStationSelected(station)
                    },
                    onFavoriteClick = { viewModel.toggleFavorite(station) },
                    modifier = Modifier.width(200.dp)
                )
            }
        }

        // Kategorie
        com.radiomaster.app.ui.component.CategoriesRow(
            categories = categories,
            selected = null,
            onCategorySelected = { category ->
                navController.navigate(Routes.Search)
            }
        )

        // Ostatnio odtwarzane
        if (recent.isNotEmpty()) {
                Text(
                text = stringResource(R.string.home_recent),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            LazyColumn {
                items(recent) { station ->
                    com.radiomaster.app.ui.component.StationListItem(
                        station = station,
                        isPlaying = playbackState == PlaybackState.READY && currentStationId == station.id,
                        onClick = {
                            viewModel.playStation(station)
                            onStationSelected(station)
                        },
                        onFavoriteClick = { viewModel.toggleFavorite(station) },
                        onRemoveClick = { /* usunięcie z ostatnich – zostawione na przyszłość */ },
                        isFavorite = station.id in favoriteIds,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
