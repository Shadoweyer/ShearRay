/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package open.shadoweyer.shearray.components

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.session.engine.EngineMiddleware
import mozilla.components.browser.session.storage.SessionStorage
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.storage.sync.PlacesHistoryStorage
import mozilla.components.browser.storage.sync.RemoteTabsStorage
import mozilla.components.browser.thumbnails.ThumbnailsMiddleware
import mozilla.components.browser.thumbnails.storage.ThumbnailStorage
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.concept.fetch.Client
import mozilla.components.feature.downloads.DownloadMiddleware
import mozilla.components.feature.media.MediaSessionFeature
import mozilla.components.feature.media.middleware.RecordingDevicesMiddleware
import mozilla.components.feature.session.HistoryDelegate
import mozilla.components.feature.sitepermissions.SitePermissionsStorage
import open.shadoweyer.shearray.AppRequestInterceptor
import open.shadoweyer.shearray.EngineProvider
import open.shadoweyer.shearray.R
import open.shadoweyer.shearray.R.string.*
import open.shadoweyer.shearray.downloads.DownloadService
import open.shadoweyer.shearray.ext.getPreferenceKey
import open.shadoweyer.shearray.media.MediaSessionService

private const val DAY_IN_MINUTES = 24 * 60L

/**
 * Component group for all core browser functionality.
 */
class Core(private val context: Context) {
    /**
     * The browser engine component initialized based on the build
     * configuration (see build variants).
     */
    val engine: Engine by lazy {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val defaultSettings = DefaultSettings(
            requestInterceptor = AppRequestInterceptor(context),
            remoteDebuggingEnabled = prefs.getBoolean(context.getPreferenceKey(pref_key_remote_debugging), false),
            testingModeEnabled = prefs.getBoolean(context.getPreferenceKey(R.string.pref_key_testing_mode), false),
            trackingProtectionPolicy = createTrackingProtectionPolicy(prefs),
            historyTrackingDelegate = HistoryDelegate(lazyHistoryStorage)
        )
        EngineProvider.createEngine(context, defaultSettings)
    }

    /**
     * The [Client] implementation (`concept-fetch`) used for HTTP requests.
     */
    val client: Client by lazy {
        EngineProvider.createClient(context)
    }

    /**
     * The [BrowserStore] holds the global [BrowserState].
     */
    val store by lazy {
        BrowserStore(
            middleware = listOf(
                DownloadMiddleware(context, DownloadService::class.java),
                ThumbnailsMiddleware(thumbnailStorage),
                RecordingDevicesMiddleware(context)
            ) + EngineMiddleware.create(engine, ::findSessionById)
        )
    }

    private fun findSessionById(tabId: String): Session? {
        return sessionManager.findSessionById(tabId)
    }

    /**
     * The session manager component provides access to a centralized registry of
     * all browser sessions (i.e. tabs). It is initialized here to persist and restore
     * sessions from the [SessionStorage], and with a default session (about:blank) in
     * case all sessions/tabs are closed.
     */
    val sessionManager by lazy {
        SessionManager(engine, store).apply {

            // Install the "icons" WebExtension to automatically load icons for every visited website.
            icons.install(engine, store)

            MediaSessionFeature(context, MediaSessionService::class.java, store).start()
        }
    }

    val sessionStorage: SessionStorage by lazy {
        SessionStorage(context, engine)
    }

    /**
     * The storage component to persist browsing history (with the exception of
     * private sessions).
     */
    val lazyHistoryStorage = lazy { PlacesHistoryStorage(context) }

    /**
     * A convenience accessor to the [PlacesHistoryStorage].
     */
    val historyStorage by lazy { lazyHistoryStorage.value }

    /**
     * The storage component to sync and persist tabs in a Firefox Sync account.
     */
    val lazyRemoteTabsStorage = lazy { RemoteTabsStorage() }

    /**
     * A storage component for persisting thumbnail images of tabs.
     */
    val thumbnailStorage by lazy { ThumbnailStorage(context) }

    /**
     * A storage component for site permissions.
     */
    val sitePermissionsStorage by lazy { SitePermissionsStorage(context) }

    /**
     * Icons component for loading, caching and processing website icons.
     */
    val icons by lazy { BrowserIcons(context, client) }

    /**
     * Constructs a [TrackingProtectionPolicy] based on current preferences.
     *
     * @param prefs the shared preferences to use when reading tracking
     * protection settings.
     * @param normalMode whether or not tracking protection should be enabled
     * in normal browsing mode, defaults to the current preference value.
     * @param privateMode whether or not tracking protection should be enabled
     * in private browsing mode, default to the current preference value.
     * @return the constructed tracking protection policy based on preferences.
     */
    fun createTrackingProtectionPolicy(
        prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context),
        normalMode: Boolean = prefs.getBoolean(context.getPreferenceKey(pref_key_tracking_protection_normal), true),
        privateMode: Boolean = prefs.getBoolean(context.getPreferenceKey(pref_key_tracking_protection_private), true)
    ): TrackingProtectionPolicy {

        val trackingPolicy = TrackingProtectionPolicy.recommended()
        return when {
            normalMode && privateMode -> trackingPolicy
            normalMode && !privateMode -> trackingPolicy.forRegularSessionsOnly()
            !normalMode && privateMode -> trackingPolicy.forPrivateSessionsOnly()
            else -> TrackingProtectionPolicy.none()
        }
    }
}
