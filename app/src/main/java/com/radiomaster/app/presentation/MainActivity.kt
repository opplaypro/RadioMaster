package com.radiomaster.app.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.radiomaster.app.RadioMasterApplication
import com.radiomaster.app.locale.AppLocale

/**
 * The only activity: an edge-to-edge Compose host plus the two things that need a window — the
 * pre-33 locale wrapper and the notification permission that makes the playback controls visible.
 */
class MainActivity : ComponentActivity() {

    private var permissionLauncher: ActivityResultLauncher<String>? = null

    override fun attachBaseContext(newBase: Context) {
        // No-op from API 33 on, where the framework applies per-app locales itself.
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

        val graph = (application as RadioMasterApplication).graph
        setContent {
            AppRoot(
                graph = graph,
                onOnboardingFinished = { askForNotificationPermission() },
            )
        }
    }

    override fun onDestroy() {
        permissionLauncher = null
        super.onDestroy()
    }

    /**
     * Media3 can show its ongoing notification from API 26 without a runtime grant, but from 33 on
     * a denied permission hides it — which also hides the lock-screen and car controls. Asking once,
     * right after onboarding, keeps the request where it is explainable.
     */
    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

}
