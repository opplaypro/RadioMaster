package com.radiomaster.app

import android.app.Application
import com.radiomaster.app.di.AppGraph

/**
 * Process entry point: builds the [AppGraph] once and warms the player up on the main looper.
 *
 * ExoPlayer binds to the thread that created it, and the screens and the media service must share
 * one instance, so it is created here rather than lazily from whichever coroutine asks first.
 */
class RadioMasterApplication : Application() {

    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
        // Touch the player now, while this is guaranteed to run on the main thread.
        graph.radioPlayback.player
    }
}
