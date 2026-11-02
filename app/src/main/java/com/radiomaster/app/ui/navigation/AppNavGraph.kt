package com.radiomaster.app.ui.navigation

import androidx.compose.material3.BottomNavigation
import androidx.compose.material3.BottomNavigationItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.vectorResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.radiomaster.app.R
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.AppSettingsRepository
import com.radiomaster.app.ui.theme.RadioMasterTheme
import com.radiomaster.app.ui.viewmodel.SettingsViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    settingsRepository: AppSettingsRepository,
    onStationSelected: (Station) -> Unit,
    onLanguageConfirmed: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.route ?: ""

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

    RadioMasterTheme(themeMode = themeMode) {
        Scaffold(
            bottomBar = if (shouldShowBottomBar(currentRoute)) {
                BottomNav(navController, currentRoute)
            } else null
        ) { _ ->
            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                startDestination = Routes.Splash
            ) {
                composable(Routes.Splash) {
                    SplashScreen(navController = navController)
                }
                composable(Routes.Onboarding) {
                    OnboardingScreen(
                        onComplete = {
                            settingsRepository.setFirstLaunch(false)
                            navController.navigate(Routes.Home)
                        },
                        onLanguageConfirmed = onLanguageConfirmed
                    )
                }
                composable(Routes.Home) {
                    LaunchedEffect(Unit) {
                        settingsRepository.setFirstLaunch(false)
                    }
                    HomeScreen(
                        navController = navController,
                        onStationSelected = onStationSelected
                    )
                }
                composable(Routes.Search) {
                    SearchScreen(
                        navController = navController,
                        onStationSelected = onStationSelected
                    )
                }
                composable(
                    Routes.StationDetail,
                    arguments = listOf(navArgument("stationId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val stationId = backStackEntry.arguments?.getString("stationId") ?: ""
                    StationDetailScreen(
                        navController = navController,
                        stationId = stationId,
                        onStationSelected = onStationSelected
                    )
                }
                composable(Routes.Favorites) {
                    FavoritesScreen(navController = navController, onStationSelected = onStationSelected)
                }
                composable(Routes.Recent) {
                    RecentScreen(navController = navController, onStationSelected = onStationSelected)
                }
                composable(Routes.Settings) {
                    SettingsScreen(
                        navController = navController,
                        onLanguageConfirmed = onLanguageConfirmed
                    )
                }
                composable(Routes.Language) {
                    LanguageScreen(
                        navController = navController,
                        onLanguageSelected = { tag ->
                            onLanguageConfirmed()
                        }
                    )
                }
            }
        }
    }
}

private fun shouldShowBottomBar(route: String): Boolean =
    route == Routes.Home || route == Routes.Search || route == Routes.Favorites ||
        route == Routes.Recent || route == Routes.Settings

@Composable
private fun BottomNav(navController: NavHostController, currentRoute: String) {
    BottomNavigation {
        Routes.bottomNavRoutes.forEach { route ->
            val selected = currentRoute == route
            BottomNavigationItem(
                icon = {
                    Icon(
                        imageVector = vectorResource(id = iconRes(route)),
                        contentDescription = labelFor(route)
                    )
                },
                label = { Text(text = labelFor(route)) },
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(route) {
                            popUpTo(Routes.Home) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}

private fun iconRes(route: String): Int = when (route) {
    Routes.Search -> R.drawable.ic_search
    Routes.Favorites -> R.drawable.ic_favorite
    Routes.Settings -> R.drawable.ic_settings
    else -> R.drawable.ic_home
}

private fun labelFor(route: String): String = when (route) {
    Routes.Search -> "Szukaj"
    Routes.Favorites -> "Ulubione"
    Routes.Settings -> "Ustawienia"
    else -> "Strona główna"
}
