package com.radiomaster.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.radiomaster.app.R
import com.radiomaster.app.domain.model.AppTheme
import com.radiomaster.app.ui.navigation.Routes
import com.radiomaster.app.ui.theme.RadioMasterTheme
import com.radiomaster.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavHostController,
    onLanguageConfirmed: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val locale by viewModel.locale.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { navController.popBackStack() }) {
                        androidx.compose.material3.Icon(
                            contentDescription = null,
                            painter = androidx.compose.ui.unit.painterResource(id = R.drawable.ic_arrow_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable {
                    navController.navigate(Routes.Language)
                },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        contentDescription = null,
                        painter = androidx.compose.ui.unit.painterResource(id = R.drawable.ic_language),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.listItemColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Column {
                    Text(text = stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = locale.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider()

            ListItem(
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.setTheme(AppTheme.SYSTEM)
                },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        contentDescription = null,
                        painter = androidx.compose.ui.unit.painterResource(id = R.drawable.ic_settings),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.listItemColors(
                    containerColor = if (themeMode == AppTheme.SYSTEM) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
                )
            ) {
                Text(text = stringResource(R.string.settings_theme_system), style = MaterialTheme.typography.titleMedium)
            }
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.setTheme(AppTheme.LIGHT)
                },
                colors = ListItemDefaults.listItemColors(
                    containerColor = if (themeMode == AppTheme.LIGHT) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
                )
            ) {
                Text(text = stringResource(R.string.settings_theme_light), style = MaterialTheme.typography.titleMedium)
            }
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.setTheme(AppTheme.DARK)
                },
                colors = ListItemDefaults.listItemColors(
                    containerColor = if (themeMode == AppTheme.DARK) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
                )
            ) {
                Text(text = stringResource(R.string.settings_theme_dark), style = MaterialTheme.typography.titleMedium)
            }

            Divider()

            ListItem(
                modifier = Modifier.fillMaxWidth().clickable { },
                leadingIcon = {
                    androidx.compose.material3.Icon(
                        contentDescription = null,
                        painter = androidx.compose.ui.unit.painterResource(id = R.drawable.ic_info),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.listItemColors(containerColor = MaterialTheme.colorScheme.background)
            ) {
                Column {
                    Text(text = stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.settings_data_source_value),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
