/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package open.shadoweyer.shearray.browser

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.browser.menu2.BrowserMenuController
import mozilla.components.browser.session.Session
import mozilla.components.browser.session.SessionManager
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.SessionState
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.display.DisplayToolbar
import mozilla.components.concept.menu.MenuController
import mozilla.components.concept.menu.candidate.*
import mozilla.components.concept.storage.HistoryStorage
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.feature.toolbar.ToolbarAutocompleteFeature
import mozilla.components.feature.toolbar.ToolbarFeature
import mozilla.components.lib.state.ext.flow
import mozilla.components.support.base.feature.LifecycleAwareFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.ktx.kotlinx.coroutines.flow.ifChanged
import open.shadoweyer.shearray.R
import open.shadoweyer.shearray.bookmark.BookmarkFragment
import open.shadoweyer.shearray.ext.components
import open.shadoweyer.shearray.ext.share
import open.shadoweyer.shearray.settings.SettingContainerFragment

class ToolbarIntegration(
        private val context: Context,
        private val toolbar: BrowserToolbar,
        historyStorage: HistoryStorage,
        sessionManager: SessionManager,
        store: BrowserStore,
        private val sessionUseCases: SessionUseCases,
        private val tabsUseCases: TabsUseCases,
        sessionId: String? = null,
        private val fragmentManager: FragmentManager?
) : LifecycleAwareFeature, UserInteractionHandler {
    private val shippedDomainsProvider = ShippedDomainsProvider().also {
        it.initialize(context)
    }

    private val scope = MainScope()

    private fun menuToolbar(session: SessionState): RowMenuCandidate {
        val tint = ContextCompat.getColor(context, R.color.icons)

        val forward = SmallMenuCandidate(
                contentDescription = "Forward",
                icon = DrawableMenuIcon(
                        context,
                        mozilla.components.ui.icons.R.drawable.mozac_ic_forward,
                        tint = tint
                ),
                containerStyle = ContainerStyle(
                        isEnabled = session.content.canGoForward == true
                )
        ) {
            sessionUseCases.goForward.invoke()
        }

        val refresh = SmallMenuCandidate(
                contentDescription = "Refresh",
                icon = DrawableMenuIcon(
                        context,
                        mozilla.components.ui.icons.R.drawable.mozac_ic_refresh,
                        tint = tint
                )
        ) {
            sessionUseCases.reload.invoke()
        }

        val stop = SmallMenuCandidate(
                contentDescription = "Stop",
                icon = DrawableMenuIcon(
                        context,
                        mozilla.components.ui.icons.R.drawable.mozac_ic_stop,
                        tint = tint
                )
        ) {
            sessionUseCases.stopLoading.invoke()
        }
        val mark = SmallMenuCandidate(
                contentDescription = "Bookmark",
                icon = DrawableMenuIcon(
                        context,
                        mozilla.components.ui.icons.R.drawable.mozac_ic_fingerprint,
                        tint = tint
                )
        ) {
            context.components.bookmarkMediator.add(session.content.url, session.content.title) {
                toolbar.title = "Added"
                delay(2000)
                toolbar.title = ""
            }
        }

        return RowMenuCandidate(listOf(forward, refresh, stop, mark))
    }

    private fun sessionMenuItems(session: Session, sessionState: SessionState): List<MenuCandidate> {
        return listOfNotNull(
                menuToolbar(sessionState),

                TextMenuCandidate("Share") {
                    val url = sessionState.content.url
                    context.share(url)
                },

                CompoundMenuCandidate(
                        text = "Request desktop site",
                        isChecked = sessionState.content.desktopMode,
                        end = CompoundMenuCandidate.ButtonType.SWITCH
                ) { checked ->
                    sessionUseCases.requestDesktopSite.invoke(checked)
                },

                TextMenuCandidate(
                        text = "Find in Page"
                ) {
                    FindInPageIntegration.launch?.invoke()
                }
        )
    }

    private fun menuItems(session: Session?, sessionState: SessionState?): List<MenuCandidate> {
        val sessionMenuItems = if (session != null && sessionState != null) {
            sessionMenuItems(session, sessionState)
        } else {
            emptyList()
        }

        return sessionMenuItems + listOf(
                TextMenuCandidate(text = "Bookmark") {
                    fragmentManager?.beginTransaction()
                            ?.setCustomAnimations(R.animator.frag_in, R.animator.frag_out, R.animator.frag_in, R.animator.frag_out)
                            ?.replace(R.id.top_container, BookmarkFragment())
                            ?.addToBackStack(null)
                            ?.commit()
                },
                TextMenuCandidate(text = "Settings") {
//                    val intent = Intent(context, SettingsActivity::class.java)
//                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                    context.startActivity(intent)
                    fragmentManager?.beginTransaction()
                            ?.replace(R.id.top_container, SettingContainerFragment())
                            ?.addToBackStack(null)
                            ?.commit()
                }
        )
    }

    private val menuController: MenuController = BrowserMenuController()

    init {
        toolbar.display.indicators = listOf(
                DisplayToolbar.Indicators.SECURITY,
                DisplayToolbar.Indicators.TRACKING_PROTECTION
        )
        toolbar.display.displayIndicatorSeparator = true
        toolbar.display.menuController = menuController

        toolbar.display.hint = context.getString(R.string.toolbar_hint)
        toolbar.edit.hint = context.getString(R.string.toolbar_hint)

        ToolbarAutocompleteFeature(toolbar).apply {
            addHistoryStorageProvider(historyStorage)
            addDomainProvider(shippedDomainsProvider)
        }

        toolbar.display.setUrlBackground(
                ResourcesCompat.getDrawable(context.resources, R.drawable.url_background, context.theme)
        )

        scope.launch {
            store.flow()
                    .map { state -> state.selectedTab }
                    .ifChanged()
                    .collect { tab ->
                        menuController.submitList(menuItems(sessionManager.selectedSession, tab))
                    }
        }
    }

    private val toolbarFeature: ToolbarFeature = ToolbarFeature(
            toolbar,
            context.components.core.store,
            context.components.useCases.sessionUseCases.loadUrl,
            { searchTerms ->
                context.components.useCases.searchUseCases.defaultSearch.invoke(
                        searchTerms = searchTerms,
                        searchEngine = null,
                        parentSessionId = null
                )
            },
            sessionId
    )

    override fun start() {
        toolbarFeature.start()
    }

    override fun stop() {
        toolbarFeature.stop()
    }

    override fun onBackPressed(): Boolean {
        return toolbarFeature.onBackPressed()
    }
}
