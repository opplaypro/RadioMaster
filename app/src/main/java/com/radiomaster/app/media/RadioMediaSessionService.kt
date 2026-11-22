package com.radiomaster.app.media

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.radiomaster.app.presentation.MainActivity
import com.radiomaster.app.R
import com.radiomaster.app.RadioMasterApplication
import com.radiomaster.app.domain.model.BrowseIds
import com.radiomaster.app.domain.model.Station
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.LinkedHashMap

/**
 * The one media session of the app, and the browse tree Android Auto renders.
 *
 * It wraps the shared [ExoPlayer][androidx.media3.exoplayer.ExoPlayer] held by
 * [RadioPlayback], so the car, the notification and the phone UI are the same controller view of
 * one player. Extending [MediaLibraryService] (rather than plain `MediaSessionService`) is what
 * makes the app appear as a *source* in the car: the callbacks below answer the browse requests,
 * and `android.media.browse.MediaBrowserService` in the manifest is what lets the car's legacy
 * `MediaBrowserCompat` client find the service at all.
 *
 * Every callback returns immediately and completes its future on the main looper after the data is
 * there — blocking the session thread would stall the car UI, and a future that never completes
 * shows as an empty list.
 */
@OptIn(UnstableApi::class)
class RadioMediaSessionService : MediaLibraryService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var librarySession: MediaLibrarySession

    private val graph get() = (application as RadioMasterApplication).graph
    private val browser get() = graph.stationBrowser

    override fun onCreate() {
        super.onCreate()
        librarySession = MediaLibrarySession.Builder(this, graph.radioPlayback.player, BrowseCallback())
            .setId(SESSION_ID)
            .setSessionActivity(mainActivityPendingIntent())
            .build()
        addSession(librarySession)
    }

    override fun onDestroy() {
        scope.cancel()
        if (::librarySession.isInitialized) {
            removeSession(librarySession)
            librarySession.release()
        }
        super.onDestroy()
    }

    /**
     * Returning a session unconditionally is deliberate. A `null` here rejects the connection, and
     * two clients need this session even though they look unusual: the legacy media-browser bridge
     * (its `ControllerInfo.packageName` is `android.service.media.MediaBrowserService.SERVICE_INTERFACE`)
     * and a headset media-button start (`Intent.ACTION_MEDIA_BUTTON`).
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        librarySession

    /** Lets a controller open the app on the playing screen instead of the home tab. */
    private fun mainActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, REQUEST_CODE_SESSION_ACTIVITY, intent, flags)
    }

    /**
     * Runs [load] on [scope] and completes the returned future with its result, so no session
     * callback ever blocks the binder thread.
     *
     * A dead mirror is a data problem, not a protocol error: `RESULT_ERROR_IO` is reported for it,
     * while cancellation — which only happens when the service is destroyed — leaves the future
     * cancelled instead of turning it into a fake error the controller would have to interpret.
     */
    private fun <V> libraryFuture(load: suspend () -> LibraryResult<V>): ListenableFuture<LibraryResult<V>> {
        val future = SettableFuture.create<LibraryResult<V>>()
        scope.launch {
            val result = try {
                load()
            } catch (e: CancellationException) {
                future.cancel(/* interrupt = */ true)
                throw e
            } catch (e: Exception) {
                LibraryResult.ofError<V>(LibraryResult.RESULT_ERROR_IO)
            }
            if (!future.isCancelled) future.set(result)
        }
        return future
    }

    private inner class BrowseCallback : MediaLibraryService.MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            controllerInfo: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
            LibraryResult.ofItem(directoryItem(BrowseIds.ROOT, getString(R.string.app_name)), params),
        )

        override fun onGetChildren(
            session: MediaLibrarySession,
            controllerInfo: MediaSession.ControllerInfo,
            parentId: String,
            pageIndex: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = libraryFuture {
            val browseId = idOfCollection(parentId)
            val offset = pageIndex.toLong() * pageSize
            val items: List<MediaItem> = when {
                browseId == BrowseIds.ROOT -> BrowseIds.rootChildren.map { directoryItem(it, titleOf(it)) }
                browseId == BrowseIds.COUNTRIES -> browser.countries().map {
                    directoryItem(BrowseIds.country(it.name), it.name)
                }
                browseId == BrowseIds.GENRES -> browser.genres().map {
                    directoryItem(BrowseIds.genre(it.tag), it.label)
                }
                offset > Int.MAX_VALUE -> emptyList()
                else -> browser.stationsOf(browseId, pageSize, offset.toInt()).map(Station::toMediaItem)
            }
            LibraryResult.ofItemList(items, params)
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            controllerInfo: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = libraryFuture {
            val station = browser.stationById(idOfStation(mediaId))
            if (station == null) {
                LibraryResult.ofError<MediaItem>(LibraryResult.RESULT_ERROR_BAD_VALUE)
            } else {
                LibraryResult.ofItem(station.toMediaItem(), /* params = */ null)
            }
        }

        override fun onSubscribe(
            session: MediaLibrarySession,
            controllerInfo: MediaSession.ControllerInfo,
            parentId: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = LibraryResult.ofVoid(params).asFuture()

        override fun onUnsubscribe(
            session: MediaLibrarySession,
            controllerInfo: MediaSession.ControllerInfo,
            parentId: String,
        ): ListenableFuture<LibraryResult<Void>> = LibraryResult.ofVoid().asFuture()

        /**
         * Voice search in the car: "play jazz radio on RadioMaster" arrives here as a query. The
         * result count is reported through [MediaLibrarySession.notifySearchResultChanged], so the
         * answer to `onSearch` is only a success marker.
         */
        override fun onSearch(
            session: MediaLibrarySession,
            controllerInfo: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = libraryFuture {
            val results = browser.search(query = query, limit = SEARCH_LIMIT)
            searchResultsByQuery[query] = results
            session.notifySearchResultChanged(controllerInfo, query, results.size, params)
            LibraryResult.ofVoid(params)
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            controllerInfo: MediaSession.ControllerInfo,
            query: String,
            pageIndex: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = libraryFuture {
            // The controller pages through the results of *this* query, so the stored list is
            // sliced rather than replayed whole.
            val results = searchResultsByQuery[query].orEmpty()
            val from = (pageIndex.toLong() * pageSize).coerceAtMost(results.size.toLong()).toInt()
            val to = (from + pageSize).coerceAtMost(results.size)
            LibraryResult.ofItemList(results.subList(from, to).map(Station::toMediaItem), params)
        }

        /**
         * Called for every `setMediaItem`/`playFromMediaId` a controller sends. Items that came
         * from the browse tree already carry a URI; an item that only holds a media id (a restored
         * queue, a deep link) is resolved from the cache here, which is what makes "play that
         * station again" work after a reboot with the app never opened.
         */
        override fun onAddMediaItems(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val future = SettableFuture.create<List<MediaItem>>()
            scope.launch {
                val resolved = mediaItems.mapNotNull { item ->
                    if (item.localConfiguration?.uri != null) item else browser.stationById(idOfStation(item.mediaId))?.toMediaItem()
                }
                if (!future.isCancelled) future.set(resolved)
            }
            return future
        }

        private fun <T> LibraryResult<T>.asFuture(): ListenableFuture<LibraryResult<T>> =
            Futures.immediateFuture(this)
    }

    /**
     * Search answers keyed by query, oldest dropped past [SEARCH_CACHE_SIZE] entries.
     *
     * Keyed rather than a single field because `onGetSearchResult` names the query it wants: with
     * one field, a car that re-pages an earlier list while a newer voice search is already running
     * would be handed the wrong stations. Written only from [scope], i.e. the main looper, like the
     * rest of the session state.
     */
    private val searchResultsByQuery =
        object : LinkedHashMap<String, List<Station>>(4, 0.75f, false) {
            override fun removeEldestEntry(eldest: Map.Entry<String, List<Station>>): Boolean =
                size > SEARCH_CACHE_SIZE
        }

    @StringRes
    private fun titleResOf(browseId: String): Int = when (browseId) {
        BrowseIds.ROOT -> R.string.app_name
        BrowseIds.FAVORITES -> R.string.library_favorites
        BrowseIds.HISTORY -> R.string.library_recents
        BrowseIds.POPULAR -> R.string.home_popular
        BrowseIds.TOP_VOTED -> R.string.home_top_voted
        BrowseIds.COUNTRIES -> R.string.home_countries
        BrowseIds.GENRES -> R.string.home_genres
        BrowseIds.RANDOM -> R.string.globe_shuffle
        else -> R.string.app_name
    }

    private fun titleOf(browseId: String): String = getString(titleResOf(browseId))

    companion object {
        private const val SESSION_ID = "radiomaster-session"
        private const val REQUEST_CODE_SESSION_ACTIVITY = 4711
        private const val SEARCH_LIMIT = 40

        /** Enough queries for a listener that scrolls back while a new search runs. */
        private const val SEARCH_CACHE_SIZE = 8
    }
}
