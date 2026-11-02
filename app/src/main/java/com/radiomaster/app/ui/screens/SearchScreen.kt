package com.radiomaster.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.ui.component.SearchBar
import com.radiomaster.app.ui.component.StationCard
import com.radiomaster.app.ui.navigation.Routes
import com.radiomaster.app.ui.theme.RadioMasterTheme
import com.radiomaster.app.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    navController: NavHostController,
    onStationSelected: (Station) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    initialQuery: String = ""
) {
    LaunchedEffect(initialQuery) {
        viewModel.setQuery(initialQuery)
    }
    val query by viewModel.query.collectAsStateWithLifecycle("")
    val results by viewModel.results.collectAsStateWithLifecycle(emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SearchBar(
            value = query,
            onValueChange = { viewModel.setQuery(it) },
            onSearch = {}
        )

        if (query.isNotBlank()) {
            if (results.isEmpty()) {
                Text(
                    text = "Nie znaleziono stacji pasujących do zapytania",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                Text(
                    text = "Znaleziono stacje",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
                )
                LazyColumn {
                    items(results) { station ->
                        StationCard(
                            station = station,
                            isFavorite = false,
                            onClick = {
                                onStationSelected(station)
                            },
                            onFavoriteClick = {},
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
