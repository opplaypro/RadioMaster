package com.radiomaster.app.presentation

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import javax.inject.Inject

abstract class AppLocaleActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val locale = (newBase.applicationContext as? App)?.currentLocale ?: java.util.Locale.getDefault()
        super.attachBaseContext(LocaleContextWrapper.wrap(newBase, locale))
    }
}
