package com.radiomaster.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.map
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.media.PlaybackState
import com.radiomaster.app.media.RadioPlayerManager
import com.radiomaster.app.ui.component.StationListItem
import com.radiomaster.app.ui.navigation.Routes
import com.radiomaster.app.ui.theme.RadioMasterTheme
import com.radiomaster.app.ui.viewmodel.HomeViewModel

@Composable
fun RecentScreen(
    navController: NavHostController,
    onStationSelected: (Station) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recent by viewModel.recentStations.collectAsStateWithLifecycle(emptyList())
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentStationId by viewModel.currentStation.map { it?.id }.collectAsStateWithLifecycle(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Ostatnio odtwarzane") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                        androidx.compose.material3.Icon(
                            contentDescription = null,
                            painter = androidx.compose.ui.unit.painterResource(id = com.radiomaster.app.R.drawable.ic_arrow_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (recent.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text(
                    text = "Nie odtwarzałeś jeszcze żadnej stacji.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                LazyColumn {
                    items(recent) { station ->
                        StationListItem(
                            station = station,
                            isPlaying = playbackState == PlaybackState.READY && currentStationId == station.id,
                            onClick = {
                                viewModel.playStation(station)
                                onStationSelected(station)
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(station) },
                            onRemoveClick = {},
                            isFavorite = station.isFavorite,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
