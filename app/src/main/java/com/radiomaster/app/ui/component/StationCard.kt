package com.radiomaster.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.media.RadioPlayerManager
import com.radiomaster.app.ui.theme.RadioSecondary
import coil.compose.rememberImagePainter
import coil.request.ImageRequest

@Composable
fun StationCard(
    station: Station,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    isFavorite: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo stacji
            val painter by rememberImagePainter(
                request = ImageRequest.Builder(LocalContext.current)
                    .data(station.faviconUrl)
                    .build()
            )
            androidx.compose.foundation.image.Image(
                painter = painter,
                contentDescription = station.name + " logo",
                modifier = Modifier.size(56.dp).clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        station.country?.let { append(it) }
                        station.language?.let { if (isNotEmpty()) append(" • $it") }
                    }.takeIf { it.isNotEmpty() } ?: "—",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = { onFavoriteClick() }) {
                Icon(
                    contentDescription = if (isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                    painter = androidx.compose.ui.unit.painterResource(id = if (isFavorite) com.radiomaster.app.R.drawable.ic_favorite_filled else com.radiomaster.app.R.drawable.ic_favorite),
                    tint = if (isFavorite) RadioSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Element listy stacji (ulubione / ostatnie) z przyciskami akcji. */
@Composable
fun StationListItem(
    station: Station,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onRemoveClick: () -> Unit,
    isFavorite: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        val painter by rememberImagePainter(
            request = ImageRequest.Builder(LocalContext.current)
                .data(station.faviconUrl)
                .build()
        )
        androidx.compose.foundation.image.Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = station.country ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isPlaying) {
            LinearProgressIndicator(modifier = Modifier.width(32.dp))
        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = { onFavoriteClick() }) {
            Icon(
                contentDescription = if (isFavorite) "Usuń z ulubionych" else "Dodaj do ulubionych",
                painter = androidx.compose.ui.unit.painterResource(id = if (isFavorite) com.radiomaster.app.R.drawable.ic_favorite_filled else com.radiomaster.app.R.drawable.ic_favorite),
                tint = if (isFavorite) RadioSecondary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = { onRemoveClick() }) {
            Icon(
                contentDescription = "Usuń z listy",
                painter = androidx.compose.ui.unit.painterResource(id = com.radiomaster.app.R.drawable.ic_radio),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
