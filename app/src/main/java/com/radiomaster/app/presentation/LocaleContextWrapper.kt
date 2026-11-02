package com.radiomaster.app.presentation

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** Obejmuje kontekst i nakazuje zasobom użycie wybranego języka aplikacji (niezależnie od systemu). */
class LocaleContextWrapper(private val base: Context, private val locale: Locale) : ContextWrapper(base) {
    override fun getResources(): Resources {
        val res = super.getResources()
        val config = Configuration(res.configuration)
        config.setLocale(locale)
        config.layoutDirection = if (locale == Locale.ROOT || locale.language == "ar" || locale.language == "he") {
            View.LAYOUT_DIRECTION_RTL
        } else View.LAYOUT_DIRECTION_LTR
        res.updateConfiguration(config, res.displayMetrics)
        return res
    }

    companion object {
        fun wrap(context: Context, locale: Locale): Context =
            LocaleContextWrapper(context, locale)
    }
}
