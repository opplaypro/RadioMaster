package com.radiomaster.app.locale

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * In-app language switching for a Compose-only app (no AppCompat).
 *
 * Two mechanisms, because the platform offers them at different levels:
 *
 *  * **API 33+** — the framework per-app locales (`LocaleManager`). The system persists the
 *    choice, recreates activities, updates the recents entry, the lock screen and Android Auto's
 *    UI language, and it is what the `android:localeConfig` manifest entry advertises.
 *  * **API 26–32** — no framework support exists, so the tag is kept in a tiny preferences file
 *    and applied by wrapping the base context of every activity in a `createConfigurationContext`
 *    with the chosen locale.
 *
 * The preferences mirror is not redundancy: [android.app.Activity.attachBaseContext] runs before
 * any coroutine, so the value has to be readable synchronously — that is also why the setting is
 * not read from DataStore here.
 */
object AppLocale {

    private const val PREFERENCES = "app_locale"
    private const val KEY_TAG = "language_tag"

    val supportedTags: List<String> = listOf("en", "pl")

    /**
     * The stored tag, or `null` for "follow the system language". A tag that this build no longer
     * ships is treated as unset, so restoring an old preference after an upgrade cannot produce a
     * context with no matching resources.
     */
    fun tag(context: Context): String? =
        preferences(context).getString(KEY_TAG, null)
            ?.takeIf { it.isNotBlank() && it in supportedTags }

    fun set(context: Context, tag: String?) {
        preferences(context).edit().putString(KEY_TAG, tag.orEmpty()).apply()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = if (tag.isNullOrBlank()) LocaleList.getEmptyLocaleList()
            else LocaleList.forLanguageTags(tag)
            // Framework per-app locales: persisted, applied to every window and surfaced in the
            // system Language screen. `applicationLocales` is an empty list for "follow system".
            val manager = context.getSystemService(android.app.locale.LocaleManager::class.java)
            if (manager != null) manager.applicationLocales = locales
        }
    }

    /** Pre-33 context wrapping; a no-op from API 33 on, where the system does this for us. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = tag(base) ?: return base
        val locale = Locale.forLanguageTag(tag)
        val configuration = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(configuration)
    }

    /** The locale the UI should currently show, resolving "system" to the device default. */
    fun resolve(context: Context): Locale {
        val tag = tag(context) ?: return Locale.getDefault()
        return Locale.forLanguageTag(tag).takeIf { it.language.isNotEmpty() } ?: Locale.getDefault()
    }

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
