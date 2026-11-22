package com.radiomaster.app.di

import android.app.Application
import com.radiomaster.app.data.local.createAppDatabase
import com.radiomaster.app.data.remote.RadioBrowserClient
import com.radiomaster.app.data.repository.LocalRepositoryImpl
import com.radiomaster.app.data.repository.SettingsRepositoryImpl
import com.radiomaster.app.data.repository.StationRepositoryImpl
import com.radiomaster.app.domain.repository.LocalRepository
import com.radiomaster.app.domain.repository.SettingsRepository
import com.radiomaster.app.domain.repository.StationRepository
import com.radiomaster.app.domain.usecase.StationBrowser
import com.radiomaster.app.media.RadioPlayback

/**
 * Hand-written dependency graph.
 *
 * A DI framework would buy little here: there are five long-lived objects, all singletons, and
 * every one of them is constructed the same way in a process. Keeping it in one file means the
 * whole object graph is reviewable at a glance, the media service can reach the *same* player
 * instance the UI uses without an entry-point dance, and the build has no annotation processor in
 * the critical path. Everything is `by lazy` so a cold start does not touch disk or open SQLite
 * before the first screen actually needs it.
 */
class AppGraph(private val application: Application) {

    val database by lazy { createAppDatabase(application) }

    val radioBrowserClient by lazy { RadioBrowserClient() }

    val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(application) }

    val localRepository: LocalRepository by lazy { LocalRepositoryImpl(database.stationDao()) }

    val stationRepository: StationRepository by lazy {
        StationRepositoryImpl(radioBrowserClient, database.stationDao())
    }

    val stationBrowser by lazy { StationBrowser(stationRepository, localRepository) }

    /**
     * Shared by the screens and by [com.radiomaster.app.media.RadioMediaSessionService]: one
     * player, one session, one source of truth for the notification and the car.
     */
    val radioPlayback: RadioPlayback by lazy {
        RadioPlayback(application, localRepository, stationRepository)
    }
}
