package com.radiomaster.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.radiomaster.app.ui.navigation.Routes
import com.radiomaster.app.ui.theme.RadioMasterTheme
import com.radiomaster.app.ui.viewmodel.SettingsViewModel
import com.radiomaster.app.domain.usecase.LocaleHelper
import java.util.Locale

@Composable
fun LanguageScreen(
    navController: NavHostController,
    onLanguageSelected: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val locale by viewModel.locale.collectAsStateWithLifecycle()

    val options = listOf(
        LanguageOption(tag = locale.toLanguageTag(), name = stringResource(R.string.language_system)),
        LanguageOption(tag = "pl", name = stringResource(R.string.language_polish)),
        LanguageOption(tag = "en", name = stringResource(R.string.language_english))
    ).distinctBy { it.tag }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.language_picker_title)) },
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
            Text(
                text = stringResource(R.string.language_picker_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, bottom = 16.dp)
            )
            options.forEach { option ->
                val selected = option.tag == locale.toLanguageTag()
                ListItem(
                    modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.setLanguage(LocaleHelper.toLocale(option.tag))
                        onLanguageSelected(option.tag)
                    },
                    leadingIcon = {
                        if (selected) {
                            Icon(
                                contentDescription = null,
                                painter = androidx.compose.ui.unit.painterResource(id = R.drawable.ic_favorite_filled),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = ListItemDefaults.listItemColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background
                    )
                ) {
                    Text(text = option.name, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

private data class LanguageOption(val tag: String, val name: String)
