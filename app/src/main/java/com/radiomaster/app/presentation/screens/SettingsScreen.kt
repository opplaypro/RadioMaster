package com.radiomaster.app.presentation.screens

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.radiomaster.app.R
import com.radiomaster.app.domain.repository.AppLanguage
import com.radiomaster.app.domain.repository.ThemeMode
import com.radiomaster.app.presentation.viewmodel.AppViewModel

/**
 * Appearance, language, and the Android Auto explanation.
 *
 * The language section deserves a word about its own implementation: from API 33 the choice is
 * written to the framework per-app locale list, so the system Settings screen shows the same value
 * and the platform recreates the window. Below API 33 there is no such hook, so this screen
 * recreates the activity and [com.radiomaster.app.locale.AppLocale] applies the locale while
 * wrapping the base context.
 */
@Composable
fun SettingsScreen(appViewModel: AppViewModel) {
    val state by appViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp),
        )

        SettingsSection(title = stringResource(R.string.settings_appearance)) {
            ThemeMode.entries.forEach { mode ->
                ChoiceRow(
                    label = stringResource(mode.labelRes()),
                    selected = state.themeMode == mode,
                    onClick = { appViewModel.setThemeMode(mode) },
                )
            }
        }

        SettingsSection(title = stringResource(R.string.settings_language)) {
            AppLanguage.entries.forEach { language ->
                ChoiceRow(
                    label = language.displayName(),
                    selected = state.language == language,
                    onClick = {
                        appViewModel.setLanguage(language)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            (context as? Activity)?.recreate()
                        }
                    },
                )
            }
            Text(
                text = stringResource(R.string.language_note),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        SettingsSection(title = stringResource(R.string.settings_auto)) {
            InfoBlock(
                iconRes = R.drawable.ic_car,
                body = stringResource(R.string.settings_auto_summary),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.settings_auto_howto),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_auto_requirements),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsSection(title = stringResource(R.string.settings_data)) {
            Text(
                text = stringResource(R.string.settings_data_summary),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        SettingsSection(title = stringResource(R.string.settings_about)) {
            Text(
                text = stringResource(R.string.settings_version, context.versionName()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.settings_attribution),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) { content() }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun InfoBlock(iconRes: Int, body: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Text(text = body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

/**
 * Language names are shown in their own language, as the system Settings screen does — "Polski"
 * must stay "Polski" when the UI is English, otherwise the row cannot be recognised.
 */
@Composable
private fun AppLanguage.displayName(): String = when (this) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    AppLanguage.ENGLISH -> "English"
    AppLanguage.POLISH -> "Polski"
}

private fun Context.versionName(): String = @Suppress("DEPRECATION") runCatching {
    packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
}.getOrDefault("")
