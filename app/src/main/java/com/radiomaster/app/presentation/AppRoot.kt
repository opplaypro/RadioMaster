package com.radiomaster.app.presentation

import android.net.Uri

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.radiomaster.app.R
import com.radiomaster.app.di.AppGraph
import com.radiomaster.app.presentation.components.NowPlayingBar
import com.radiomaster.app.presentation.screens.GlobeScreen
import com.radiomaster.app.presentation.screens.HomeScreen
import com.radiomaster.app.presentation.screens.LibraryScreen
import com.radiomaster.app.presentation.screens.SearchScreen
import com.radiomaster.app.presentation.screens.SettingsScreen
import com.radiomaster.app.presentation.screens.StationListScreen
import com.radiomaster.app.presentation.viewmodel.AppViewModel
import com.radiomaster.app.presentation.viewmodel.LibraryViewModel
import com.radiomaster.app.presentation.viewmodel.PlaybackViewModel
import com.radiomaster.app.presentation.viewmodel.RadioViewModelFactory
import com.radiomaster.app.ui.theme.RadioMasterTheme

/** Routes of the tab graph. */
object Routes {
    const val GLOBE = "globe"
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"

    /**
     * A category page. The browse id travels as a query parameter, not a path segment, because
     * `country/Poland` and tags such as `adult hits` contain both a slash and a space.
     */
    const val LIST = "list"
    const val LIST_ARG = "browseId"

    /**
     * The id is percent-encoded because `country/Poland` and tags such as `adult hits` contain a
     * slash and a space, both of which a route string cannot carry raw.
     */
    fun list(browseId: String): String = "$LIST?$LIST_ARG=${Uri.encode(browseId)}"

    /**
     * Decodes the argument only when it still looks percent-encoded. `NavDeepLink` hands back
     * whatever the URI parser produced, which already resolves `%20` on some versions, and applying
     * `Uri.decode` twice would corrupt a name that legitimately contains a percent sign — so the
     * extra step is taken only when it is actually needed.
     */
    fun listIdOf(encoded: String?): String =
        encoded?.takeIf { PercentPattern.containsMatchIn(it) }?.let(Uri::decode) ?: encoded.orEmpty()

    private val PercentPattern = Regex("%[0-9A-Fa-f]{2}")
}

/**
 * Root composable: waits for the stored preferences, then shows either onboarding or the tab shell.
 *
 * The three view models that must outlive a tab change (settings, playback, library) are created
 * here, where the owner is the activity, and passed down. Calling `viewModel()` inside a
 * destination instead would scope them to that nav back stack entry and lose the playback state on
 * every tab switch.
 */
@Composable
fun AppRoot(graph: AppGraph, onOnboardingFinished: () -> Unit) {
    val factory = remember(graph) { RadioViewModelFactory(graph) }
    val appViewModel: AppViewModel = viewModel(factory = factory)
    val playbackViewModel: PlaybackViewModel = viewModel(factory = factory)
    val libraryViewModel: LibraryViewModel = viewModel(factory = factory)
    val state by appViewModel.state.collectAsStateWithLifecycle()

    RadioMasterTheme(themeMode = state.themeMode) {
        when (state.onboarded) {
            // Unknown yet: a bare spinner beats flashing the first-run page to returning users.
            null -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator()
            }

            false -> OnboardingScreen(onContinue = {
                appViewModel.finishOnboarding()
                onOnboardingFinished()
            })

            true -> Shell(playbackViewModel, libraryViewModel, appViewModel, factory)
        }
    }
}

private data class Tab(val route: String, val labelRes: Int, val iconRes: Int)

@Composable
private fun Shell(
    playbackViewModel: PlaybackViewModel,
    libraryViewModel: LibraryViewModel,
    appViewModel: AppViewModel,
    factory: RadioViewModelFactory,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val playbackState by playbackViewModel.state.collectAsStateWithLifecycle()

    val tabs = listOf(
        Tab(Routes.GLOBE, R.string.nav_globe, R.drawable.ic_radio),
        Tab(Routes.HOME, R.string.nav_home, R.drawable.ic_home),
        Tab(Routes.SEARCH, R.string.nav_search, R.drawable.ic_search),
        Tab(Routes.LIBRARY, R.string.nav_library, R.drawable.ic_library_music),
        Tab(Routes.SETTINGS, R.string.nav_settings, R.drawable.ic_settings),
    )

    Scaffold(
        bottomBar = {
            Column {
                AnimatedVisibility(
                    visible = !playbackState.isIdle,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    NowPlayingBar(
                        state = playbackState,
                        onPlayPause = playbackViewModel::toggle,
                        onStop = playbackViewModel::stop,
                        onRetry = playbackViewModel::retry,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = {
                                Icon(painter = painterResource(tab.iconRes), contentDescription = null)
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.GLOBE,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.GLOBE) {
                GlobeScreen(
                    browseViewModel = viewModel(factory = factory),
                    playbackViewModel = playbackViewModel,
                    libraryViewModel = libraryViewModel,
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    browseViewModel = viewModel(factory = factory),
                    playbackViewModel = playbackViewModel,
                    libraryViewModel = libraryViewModel,
                    onOpenCategory = { browseId -> navController.navigate(Routes.list(browseId)) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    browseViewModel = viewModel(factory = factory),
                    playbackViewModel = playbackViewModel,
                    libraryViewModel = libraryViewModel,
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    libraryViewModel = libraryViewModel,
                    playbackViewModel = playbackViewModel,
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(appViewModel = appViewModel)
            }
            composable("${Routes.LIST}?${Routes.LIST_ARG}={${Routes.LIST_ARG}}") { entry ->
                val browseId = Routes.listIdOf(entry.arguments?.getString(Routes.LIST_ARG))
                StationListScreen(
                    browseId = browseId,
                    browseViewModel = viewModel(factory = factory),
                    playbackViewModel = playbackViewModel,
                    libraryViewModel = libraryViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/** Single-top tab switching: the previous tab keeps its state, and a repeat tap adds no entry. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun OnboardingScreen(onContinue: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_radio),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(12.dp))
            listOf(
                stringResource(R.string.globe_hint),
                stringResource(R.string.library_offline_note),
                stringResource(R.string.settings_auto_summary),
            ).forEach { line ->
                Text(
                    text = "•  $line",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.globe_play_selected))
            }
        }
    }
}
