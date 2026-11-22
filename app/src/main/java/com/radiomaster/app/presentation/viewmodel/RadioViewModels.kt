package com.radiomaster.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.radiomaster.app.di.AppGraph
import com.radiomaster.app.domain.model.BrowseIds
import com.radiomaster.app.domain.model.Country
import com.radiomaster.app.domain.model.Genre
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.AppLanguage
import com.radiomaster.app.domain.repository.LocalRepository
import com.radiomaster.app.domain.repository.SettingsRepository
import com.radiomaster.app.domain.repository.ThemeMode
import com.radiomaster.app.domain.usecase.StationBrowser
import com.radiomaster.app.media.PlaybackUiState
import com.radiomaster.app.media.RadioPlayback
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

/**
 * App-wide state: appearance, language, and whether the first-run flow is finished.
 *
 * [AppState.onboarded] is nullable so the window can wait for the stored preference instead of
 * flashing the onboarding page at returning users on every cold start.
 */
data class AppState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val onboarded: Boolean? = null,
)

class AppViewModel(private val settings: SettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch { settings.themeMode.collect { mode -> _state.update { it.copy(themeMode = mode) } } }
        viewModelScope.launch { settings.appLanguage.collect { lang -> _state.update { it.copy(language = lang) } } }
        viewModelScope.launch { settings.onboarded.collect { done -> _state.update { it.copy(onboarded = done) } } }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }

    fun setLanguage(language: AppLanguage) = viewModelScope.launch { settings.setAppLanguage(language) }

    fun finishOnboarding() = viewModelScope.launch { settings.setOnboarded() }
}

/** Playback snapshot and controls; the same state the notification and the car show. */
class PlaybackViewModel(private val playback: RadioPlayback) : ViewModel() {

    val state: StateFlow<PlaybackUiState> = playback.state

    fun play(station: Station) = playback.play(station)
    fun toggle() = playback.togglePlayPause()
    fun stop() = playback.stop()
    fun retry() = playback.retry()
}

/**
 * Favorites + history. Kept at activity scope by the caller, so a star tapped on one screen is
 * reflected in every other list without any explicit cache invalidation: Room re-emits the query
 * and the collectors re-compose.
 */
class LibraryViewModel(private val local: LocalRepository) : ViewModel() {

    val favorites: Flow<List<Station>> = local.observeFavorites()
    val history: Flow<List<Station>> = local.observeHistory()

    fun isFavorite(stationId: String): Flow<Boolean> = local.isFavorite(stationId)

    fun toggleFavorite(station: Station) = viewModelScope.launch { local.toggleFavorite(station) }

    fun clearHistory() = viewModelScope.launch { local.clearHistory() }
}

/** What a catalogue screen shows: a list of stations, or the home page's four sections. */
data class BrowseUiState(
    val isLoading: Boolean = false,
    val failed: Boolean = false,
    val stations: List<Station> = emptyList(),
    val popular: List<Station> = emptyList(),
    val topVoted: List<Station> = emptyList(),
    val countries: List<Country> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val directoriesLoaded: Boolean = false,
    val query: String = "",
)

/**
 * Drives every catalogue-backed screen (home, category list, search, globe) with one state object.
 *
 * Screens share it because they all ask the same question — "which stations belong to this browse
 * id" — and that question is also what the Android Auto browse tree answers, so the answer only
 * has to be written once, in [StationBrowser].
 */
class BrowseViewModel(private val browser: StationBrowser) : ViewModel() {

    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    private var openedId: String? = null
    private var homeRequested = false

    fun open(browseId: String) {
        if (openedId == browseId) return
        openedId = browseId
        load { browser.stationsOf(browseId) }
    }

    fun search(query: String, country: String? = null, genre: String? = null) {
        _state.update { it.copy(query = query) }
        if (query.isBlank() && country.isNullOrBlank() && genre.isNullOrBlank()) {
            openedId = null
            _state.update { it.copy(stations = emptyList(), isLoading = false, failed = false) }
            return
        }
        openedId = null
        load { browser.search(query = query, country = country, genre = genre) }
    }

    /** The globe asks for the circle that covers what is currently on screen. */
    fun nearby(latitude: Double, longitude: Double, radiusKm: Double) {
        openedId = null
        load { browser.nearby(latitude, longitude, radiusKm) }
    }

    fun loadHome() {
        if (homeRequested) return
        homeRequested = true
        _state.update { it.copy(isLoading = true, failed = false) }
        viewModelScope.launch {
            try {
                // Four independent requests, so a slow mirror does not serialise the home page.
                val popular = async { browser.stationsOf(BrowseIds.POPULAR, limit = 20) }
                val topVoted = async { browser.stationsOf(BrowseIds.TOP_VOTED, limit = 20) }
                val countries = async { browser.countries() }
                val genres = async { browser.genres() }
                _state.update {
                    it.copy(
                        isLoading = false,
                        failed = false,
                        popular = popular.await(),
                        topVoted = topVoted.await(),
                        countries = countries.await(),
                        genres = genres.await(),
                        directoriesLoaded = true,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, failed = true) }
            }
        }
    }

    /** The directory's own shuffle (`order=random`); null when no mirror answered. */
    suspend fun randomStation(): Station? = try {
        browser.stationsOf(BrowseIds.RANDOM, limit = 1).firstOrNull()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    /** Lets the home page retry after a failure without re-entering the screen. */
    fun retryHome() {
        homeRequested = false
        loadHome()
    }

    /** Re-runs the last browse request, e.g. after a favorite was toggled on a list screen. */
    fun refresh() {
        openedId?.let { id ->
            openedId = null
            open(id)
        }
    }

    private fun load(block: suspend () -> List<Station>) {
        _state.update { it.copy(isLoading = true, failed = false) }
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = false, failed = false, stations = block()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, failed = true, stations = emptyList()) }
            }
        }
    }

}

/**
 * Manual ViewModel factory.
 *
 * Constructor injection would need a DI framework; with five view models and one object graph the
 * explicit `when` is shorter than the configuration would be, and every dependency is visible at
 * the call site.
 */
class RadioViewModelFactory(private val graph: AppGraph) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AppViewModel::class.java) -> AppViewModel(graph.settingsRepository) as T
        modelClass.isAssignableFrom(PlaybackViewModel::class.java) -> PlaybackViewModel(graph.radioPlayback) as T
        modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(graph.localRepository) as T
        modelClass.isAssignableFrom(BrowseViewModel::class.java) -> BrowseViewModel(graph.stationBrowser) as T
        else -> throw IllegalArgumentException("unsupported view model: ${modelClass.name}")
    }
}
