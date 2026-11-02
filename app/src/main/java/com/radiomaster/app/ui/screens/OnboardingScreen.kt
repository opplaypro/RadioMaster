package com.radiomaster.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.radiomaster.app.R
import com.radiomaster.app.ui.navigation.Routes
import com.radiomaster.app.ui.theme.RadioMasterTheme

private val pages = listOf(
    OnboardingPage(
        titleRes = R.string.onboarding_find_title,
        descRes = R.string.onboarding_find_desc,
        iconRes = R.drawable.ic_globe
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_listen_title,
        descRes = R.string.onboarding_listen_desc,
        iconRes = R.drawable.ic_radio
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_favorite_title,
        descRes = R.string.onboarding_favorite_desc,
        iconRes = R.drawable.ic_favorite
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_auto_title,
        descRes = R.string.onboarding_auto_desc,
        iconRes = R.drawable.ic_home
    )
)

data class OnboardingPage(val titleRes: Int, val descRes: Int, val iconRes: Int)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val page by remember { mutableStateOf(0) }
    val lastPage = page == pages.size - 1

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val current = pages[page]
                androidx.compose.foundation.image.Image(
                    painter = androidx.compose.ui.unit.painterResource(id = current.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    text = stringResource(current.titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 24.dp)
                )
                Text(
                    text = stringResource(current.descRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, bottom = 24.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                if (page > 0) {
                    TextButton(
                        onClick = { page = page - 1 },
                        colors = TextButtonDefaults.textButtonColors()
                    ) {
                        Text(text = stringResource(R.string.onboarding_skip))
                    }
                }
                Text(
                    text = "${page + 1} / ${pages.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (lastPage) {
                    Button(
                        onClick = { onComplete() },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = stringResource(R.string.onboarding_done))
                    }
                } else {
                    Button(
                        onClick = { page = page + 1 },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = stringResource(R.string.onboarding_next))
                    }
                }
            }
        }
    }
}
