package com.radiomaster.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.radiomaster.app.domain.model.Station
import com.radiomaster.app.domain.repository.AppSettingsRepository
import com.radiomaster.app.media.RadioPlayerManager
import com.radiomaster.app.ui.navigation.AppNavGraph
import com.radiomaster.app.ui.navigation.Routes

@AndroidEntryPoint
class MainActivity : AppLocaleActivity() {

    @Inject lateinit var playerManager: RadioPlayerManager
    @Inject lateinit var settingsRepository: AppSettingsRepository

    private val settingsViewModel by hiltViewModels<SettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Zmiana języka aplikacji wymaga ponownego utworzenia aktywności, aby zasoby (strings) zostały wczytane ponownie.
        LaunchedEffect(settingsViewModel.localeChangeRequested) {
            settingsViewModel.localeChangeRequested.collect { locale ->
                recreate()
            }
        }

        setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            AppNavGraph(
                navController = navController,
                settingsRepository = settingsRepository,
                onStationSelected = { station: Station ->
                    playerManager.play(station)
                    navController.navigate("station/${station.id}")
                },
                onLanguageConfirmed = {}
            )
        }
    }
}
