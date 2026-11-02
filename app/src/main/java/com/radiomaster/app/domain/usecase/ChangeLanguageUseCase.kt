package com.radiomaster.app.domain.usecase

import com.radiomaster.app.domain.repository.AppSettingsRepository
import java.util.Locale

class ChangeLanguageUseCase(private val repository: AppSettingsRepository) {
    suspend operator fun invoke(languageTag: String) {
        val locale = LocaleHelper.toLocale(languageTag)
        repository.setLocale(locale)
    }
    val locales: List<String> = listOf(LocaleHelper.toTag(Locale.getDefault()), "pl", "en").distinct()
}

object LocaleHelper {
    fun toLocale(tag: String): Locale = when {
        tag.contains("-") -> {
            val parts = tag.split("-")
            if (parts.size >= 2) Locale(parts[0], parts[1]) else Locale(tag)
        }
        else -> Locale(tag)
    }
    fun toTag(locale: Locale): String = locale.toLanguageTag()
}
