package com.radiomaster.app.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiomaster.app.R
import com.radiomaster.app.domain.model.MapPosition
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.presentation.components.StationTile
import com.radiomaster.app.presentation.viewmodel.BrowseViewModel
import com.radiomaster.app.presentation.viewmodel.LibraryViewModel
import com.radiomaster.app.presentation.viewmodel.PlaybackViewModel
import com.radiomaster.app.ui.globe.WorldBackdrop
import com.radiomaster.app.ui.globe.WorldBackdropLoader
import com.radiomaster.app.ui.globe.pathFor
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The map tab: an offline equirectangular world with one dot per station in the visible area.
 *
 * The projection stays flat instead of rendering a sphere: it is the projection the data is stored
 * in, so a dot is `x = (lon + 180) / 360`, `y = (90 - lat) / 180` with no trigonometry and no pole
 * distortion, and the rings need no clipping because the generator already split polygons at the
 * antimeridian. The gesture layer maintains only a scale and a translation, which covers "pan to a
 * country, tap a dot, pick a station" — the interaction people actually perform.
 */
@Composable
fun GlobeScreen(
    browseViewModel: BrowseViewModel,
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val browseState by browseViewModel.state.collectAsStateWithLifecycle()
    val playback by playbackViewModel.state.collectAsStateWithLifecycle()
    val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle(initialValue = emptyList())

    var backdrop by remember { mutableStateOf<WorldBackdrop?>(null) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }
    var selected by remember { mutableStateOf<List<Station>>(emptyList()) }

    LaunchedEffect(Unit) { backdrop = WorldBackdropLoader.load(context) }

    val touchRadiusPx = with(LocalDensity.current) { 26.dp.toPx() }
    val landColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val outlineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val dotColor = MaterialTheme.colorScheme.primary
    val activeColor = MaterialTheme.colorScheme.secondary
    val selectedIds = remember(selected) { selected.map(Station::id).toSet() }

    // One request per gesture: the query waits until the map stops moving.
    LaunchedEffect(scale, translation, viewport) {
        if (viewport.width == 0 || viewport.height == 0) return@LaunchedEffect
        delay(SETTLE_MILLIS)
        val view = MapView(viewport.width.toFloat(), viewport.height.toFloat(), scale, translation)
        browseViewModel.nearby(view.centreLatitude, view.centreLongitude, view.radiusKm)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .onSizeChanged { viewport = it }
                .pointerInput(viewport) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
                        val nextTranslation = clampTranslation(
                            viewport = IntSize(size.width, size.height),
                            scale = nextScale,
                            proposed = translation + pan,
                        )
                        scale = nextScale
                        translation = nextTranslation
                        selected = emptyList()
                    }
                }
                // Keyed on the data only: scale and translation are read through their state
                // holders inside the lambda, so a pan does not have to restart the tap detector.
                .pointerInput(viewport, browseState.stations) {
                    detectTapGestures(onTap = { position ->
                        val view = MapView(size.width.toFloat(), size.height.toFloat(), scale, translation)
                        selected = browseState.stations.nearDot(view, position, touchRadiusPx)
                    })
                },
        ) {
            val view = MapView(size.width, size.height, scale, translation)
            drawMap(
                view = view,
                backdrop = backdrop,
                stations = browseState.stations,
                selectedIds = selectedIds,
                currentId = playback.station?.id,
                landColor = landColor,
                outlineColor = outlineColor,
                dotColor = dotColor,
                activeColor = activeColor,
            )
        }

        MapControls(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            canReset = scale != 1f || translation != Offset.Zero,
            onReset = {
                scale = 1f
                translation = Offset.Zero
                selected = emptyList()
            },
            onShuffle = {
                scope.launch {
                    val station = browseViewModel.randomStation()
                    if (station != null) playbackViewModel.play(station)
                }
            },
        )

        MapStatus(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            isLoading = browseState.isLoading,
            failed = browseState.failed,
            count = browseState.stations.size,
        )

        if (selected.isNotEmpty()) {
            SelectedStations(
                modifier = Modifier.align(Alignment.BottomCenter),
                stations = selected,
                favoriteIds = remember(favorites) { favorites.map(Station::id).toSet() },
                currentId = playback.station?.id,
                onPlay = { station ->
                    if (playback.station?.id == station.id) playbackViewModel.toggle()
                    else playbackViewModel.play(station)
                },
                onToggleFavorite = { station -> libraryViewModel.toggleFavorite(station) },
                onDismiss = { selected = emptyList() },
            )
        }
    }
}

private const val MAX_SCALE = 9f
private const val SETTLE_MILLIS = 380L

/**
 * Screen transform of the map: the world rect is `viewportWidth * scale` wide and half as tall
 * (2:1 equirectangular), centred in the viewport and moved by [translation].
 */
private class MapView(
    val viewportWidth: Float,
    val viewportHeight: Float,
    val scale: Float,
    val translation: Offset,
) {
    val worldWidth: Float get() = viewportWidth * scale
    val worldHeight: Float get() = worldWidth / 2f

    /** Top-left corner of the world rect, in screen pixels. */
    val origin: Offset
        get() = Offset(
            x = (viewportWidth - worldWidth) / 2f + translation.x,
            y = (viewportHeight - worldHeight) / 2f + translation.y,
        )

    fun toScreen(position: MapPosition): Offset =
        Offset(origin.x + position.x * worldWidth, origin.y + position.y * worldHeight)

    /** `latitude to longitude` of a screen point. */
    fun toDegrees(position: Offset): Pair<Double, Double> {
        val nx = (position.x - origin.x) / worldWidth
        val ny = (position.y - origin.y) / worldHeight
        return Pair(90.0 - ny * 180.0, nx * 360.0 - 180.0)
    }

    val centreDegrees: Pair<Double, Double>
        get() = toDegrees(Offset(viewportWidth / 2f, viewportHeight / 2f))

    /**
     * Circle covering the viewport. The directory takes `geo_distance` instead of a bounding box,
     * so the visible rectangle is approximated by its circumcircle — one request per view.
     */
    val radiusKm: Double
        get() {
            val longitudeSpan = 360.0 * viewportWidth / worldWidth
            val latitudeSpan = 180.0 * viewportHeight / worldHeight
            val spanKm = hypot(longitudeSpan, latitudeSpan) * KILOMETERS_PER_DEGREE / 2.0
            return spanKm.coerceIn(150.0, 20_000.0)
        }

    companion object {
        private const val KILOMETERS_PER_DEGREE = 111.32
    }
}

/** Keeps the map from being dragged off screen; at scale 1 the world fills the width, so it is fixed. */
private fun clampTranslation(viewport: IntSize, scale: Float, proposed: Offset): Offset {
    val worldWidth = viewport.width * scale
    val worldHeight = worldWidth / 2f
    val maxX = ((worldWidth - viewport.width) / 2f).coerceAtLeast(0f)
    val maxY = ((worldHeight - viewport.height) / 2f).coerceAtLeast(0f)
    return Offset(
        x = proposed.x.coerceIn(-maxX, maxX),
        y = proposed.y.coerceIn(-maxY, maxY),
    )
}

private fun List<Station>.nearDot(view: MapView, touch: Offset, radiusPx: Float): List<Station> {
    var nearest = Float.MAX_VALUE
    for (station in this) {
        val position = station.mapPosition ?: continue
        val distance = (view.toScreen(position) - touch).getDistance()
        if (distance < nearest) nearest = distance
    }
    if (nearest > radiusPx) return emptyList()
    // A dot often stands for several stations in one city, so the whole cluster is offered.
    val (touchLatitude, touchLongitude) = view.toDegrees(touch)
    val (edgeLatitude, edgeLongitude) = view.toDegrees(touch + Offset(radiusPx, radiusPx))
    val latSpan = abs(edgeLatitude - touchLatitude)
    val lonSpan = abs(edgeLongitude - touchLongitude)
    return filter { station ->
        val position = station.mapPosition ?: return@filter false
        abs(position.latitude - touchLatitude) <= latSpan &&
            abs(position.longitude - touchLongitude) <= lonSpan
    }
        .sortedByDescending { it.votes }
        .take(MAX_SELECTED)
}

private const val MAX_SELECTED = 6

private fun DrawScope.drawMap(
    view: MapView,
    backdrop: WorldBackdrop?,
    stations: List<Station>,
    selectedIds: Set<String>,
    currentId: String?,
    landColor: Color,
    outlineColor: Color,
    dotColor: Color,
    activeColor: Color,
) {
    for (ring in backdrop?.polygons.orEmpty()) {
        val path = pathFor(
            ring = ring,
            originX = view.origin.x,
            originY = view.origin.y,
            worldWidth = view.worldWidth,
            worldHeight = view.worldHeight,
        )
        drawPath(path = path, color = landColor)
        drawPath(path = path, color = outlineColor, style = Stroke(width = 1f))
    }

    for (station in stations) {
        val position = station.mapPosition ?: continue
        val centre = view.toScreen(position)
        if (centre.x < -8f || centre.y < -8f ||
            centre.x > view.viewportWidth + 8f || centre.y > view.viewportHeight + 8f
        ) {
            continue
        }
        val isSelected = station.id in selectedIds
        val isCurrent = station.id == currentId
        val radius = if (isCurrent) 9f else if (isSelected) 7f else 4.5f
        if (isCurrent || isSelected) {
            drawCircle(color = activeColor.copy(alpha = 0.25f), radius = radius + 6f, centre = centre)
        }
        drawCircle(
            color = if (isCurrent) activeColor else dotColor,
            radius = radius,
            centre = centre,
        )
    }
}

@Composable
private fun MapControls(modifier: Modifier, canReset: Boolean, onReset: () -> Unit, onShuffle: () -> Unit) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MapButton(iconRes = R.drawable.ic_shuffle, labelRes = R.string.globe_shuffle, onClick = onShuffle)
        if (canReset) {
            MapButton(iconRes = R.drawable.ic_refresh, labelRes = R.string.globe_reset_view, onClick = onReset)
        }
    }
}

@Composable
private fun MapButton(iconRes: Int, labelRes: Int, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = stringResource(labelRes),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun MapStatus(modifier: Modifier, isLoading: Boolean, failed: Boolean, count: Int) {
    val text = when {
        isLoading -> stringResource(R.string.globe_loading)
        failed -> stringResource(R.string.status_error_generic)
        count == 0 -> stringResource(R.string.globe_no_geo)
        else -> stringResource(R.string.globe_stations_shown, count)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SelectedStations(
    modifier: Modifier,
    stations: List<Station>,
    favoriteIds: Set<String>,
    currentId: String?,
    onPlay: (Station) -> Unit,
    onToggleFavorite: (Station) -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.globe_selected_count, stations.size),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.common_close),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            LazyColumn(modifier = Modifier.height((stations.size.coerceAtMost(4) * 56).dp)) {
                items(items = stations, key = { station -> station.id }) { station ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { onPlay(station) })
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StationTile(station = station, size = 32)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = station.name,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (station.locationLabel.isNotEmpty()) {
                                Text(
                                    text = station.locationLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                        IconButton(onClick = { onToggleFavorite(station) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                painter = painterResource(
                                    if (station.id in favoriteIds) R.drawable.ic_favorite
                                    else R.drawable.ic_favorite_border,
                                ),
                                contentDescription = stringResource(R.string.cd_favorite),
                            )
                        }
                        Icon(
                            painter = painterResource(
                                if (station.id == currentId) R.drawable.ic_pause else R.drawable.ic_play,
                            ),
                            contentDescription = stringResource(R.string.globe_play_selected),
                            modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}
