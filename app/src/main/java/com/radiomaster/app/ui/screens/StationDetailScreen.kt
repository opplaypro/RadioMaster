package com.radiomaster.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.radiomaster.app.R
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.media.PlaybackState
import com.radiomaster.app.ui.theme.RadioMasterTheme
import com.radiomaster.app.ui.viewmodel.StationDetailViewModel
import coil.compose.rememberImagePainter
import coil.request.ImageRequest

@Composable
fun StationDetailScreen(
    navController: NavHostController,
    stationId: String,
    onStationSelected: (Station) -> Unit,
    viewModel: StationDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(stationId) {
        viewModel.loadStation(stationId)
    }

    val station by viewModel.station.collectAsStateWithLifecycle(null)
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle(false)
    val currentStationId by viewModel.currentStationId.collectAsStateWithLifecycle(null)
    val isFavorite by viewModel.isFavoriteFlow(stationId).collectAsStateWithLifecycle(false)

    val isThisStationPlaying = currentStationId == stationId && isPlaying
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = station?.name ?: "") },
                colors = MaterialTheme.topAppBarDefaults,
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            contentDescription = null,
                            painter = androidx.compose.ui.unit.painterResource(id = R.drawable.ic_arrow_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (station == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Okładka / logo stacji
                val painter by rememberImagePainter(
                    request = ImageRequest.Builder(context)
                        .data(station.faviconUrl)
                        .build()
                )
                androidx.compose.foundation.image.Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .align(Alignment.CenterHorizontally)
                )

                Text(
                    text = station.name,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                Row(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = station.country ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    if (station.language != null && station.language.isNotEmpty()) {
                        Text(text = " • ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = station.language,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(horizontal = 32.dp, top = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (station.codec != null) {
                        Text(text = station.codec, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "  •  ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = station.displayBitrate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Tagi
                if (station.displayTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, top = 16.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        station.displayTags.forEach { tag ->
                            androidx.compose.material3.Chip(
                                onClick = {},
                                label = { Text(text = tag) },
                                colors = androidx.compose.material3.ChipDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(24.dp))

                // Przycisk odtwarzania / pauzy
                Button(
                    onClick = {
                        if (isThisStationPlaying) {
                            viewModel.togglePlayback()
                        } else {
                            viewModel.play()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(
                        text = if (isThisStationPlaying) "Wstrzymaj" else "Odtwórz",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (playbackState == PlaybackState.CONNECTING || playbackState == PlaybackState.BUFFERING) {
                    LinearProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                } else if (playbackState == PlaybackState.ERROR) {
                    Text(
                        text = "Błąd odtwarzania strumienia",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        enabled = station != null
                    ) {
                        Icon(
                            contentDescription = if (isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                            painter = androidx.compose.ui.unit.painterResource(
                                id = if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite
                            ),
                            tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { shareStation(context, station) }) {
                        Icon(
                            contentDescription = "Udostępnij",
                            painter = androidx.compose.ui.unit.painterResource(id = R.drawable.ic_share),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (station.homepage != null) {
                        IconButton(onClick = { openWeb(context, station.homepage!!) }) {
                        Icon(
                            contentDescription = "Strona stacji",
                            painter = androidx.compose.ui.unit.painterResource(id = R.drawable.ic_globe),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun shareStation(context: android.content.Context, station: Station) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.station_share_text, station.name, station.url))
    }
    context.startActivity(Intent.createChooser(intent, null))
}

private fun openWeb(context: android.content.Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) })
    } catch (e: Exception) {
        // Brak przeglądarki – ignorowane w tym szkicu
    }
}
