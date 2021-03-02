/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package open.shadoweyer.shearray.browser

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.fragment_browser.*
import kotlinx.android.synthetic.main.fragment_browser.view.*
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.toolbar.behavior.BrowserToolbarBehavior
import mozilla.components.feature.downloads.DownloadsFeature
import mozilla.components.feature.downloads.manager.FetchDownloadManager
import mozilla.components.feature.findinpage.view.FindInPageView
import mozilla.components.feature.prompts.PromptFeature
import mozilla.components.feature.session.FullScreenFeature
import mozilla.components.feature.session.SessionFeature
import mozilla.components.feature.session.SwipeRefreshFeature
import mozilla.components.feature.session.behavior.EngineViewBrowserToolbarBehavior
import mozilla.components.feature.sitepermissions.SitePermissionsFeature
import mozilla.components.feature.tabs.WindowFeature
import mozilla.components.support.base.feature.PermissionsFeature
import mozilla.components.support.base.feature.UserInteractionHandler
import mozilla.components.support.base.feature.ViewBoundFeatureWrapper
import mozilla.components.support.base.log.logger.Logger
import mozilla.components.support.ktx.android.view.enterToImmersiveMode
import mozilla.components.support.ktx.android.view.exitImmersiveModeIfNeeded
import open.shadoweyer.shearray.AppPermissionCodes.REQUEST_CODE_APP_PERMISSIONS
import open.shadoweyer.shearray.AppPermissionCodes.REQUEST_CODE_DOWNLOAD_PERMISSIONS
import open.shadoweyer.shearray.AppPermissionCodes.REQUEST_CODE_PROMPT_PERMISSIONS
import open.shadoweyer.shearray.R
import open.shadoweyer.shearray.downloads.DownloadService
import open.shadoweyer.shearray.ext.requireComponents
import mozilla.components.browser.toolbar.behavior.ToolbarPosition as MozacToolbarBehaviorToolbarPosition
import mozilla.components.feature.session.behavior.ToolbarPosition as MozacEngineBehaviorToolbarPosition

/**
 * Base fragment extended by [BrowserFragment] and [ExternalAppBrowserFragment].
 * This class only contains shared code focused on the main browsing content.
 * UI code specific to the app or to custom tabs can be found in the subclasses.
 */
@Suppress("TooManyFunctions")
abstract class BaseBrowserFragment : Fragment(), UserInteractionHandler {
    private val sessionFeature = ViewBoundFeatureWrapper<SessionFeature>()
    private val toolbarIntegration = ViewBoundFeatureWrapper<ToolbarIntegration>()
    private val contextMenuIntegration = ViewBoundFeatureWrapper<ContextMenuIntegration>()
    private val downloadsFeature = ViewBoundFeatureWrapper<DownloadsFeature>()
    private val promptsFeature = ViewBoundFeatureWrapper<PromptFeature>()
    private val fullScreenFeature = ViewBoundFeatureWrapper<FullScreenFeature>()
    private val findInPageIntegration = ViewBoundFeatureWrapper<FindInPageIntegration>()
    private val sitePermissionFeature = ViewBoundFeatureWrapper<SitePermissionsFeature>()
    private val swipeRefreshFeature = ViewBoundFeatureWrapper<SwipeRefreshFeature>()
    private val windowFeature = ViewBoundFeatureWrapper<WindowFeature>()

    private val backButtonHandler: List<ViewBoundFeatureWrapper<*>> = listOf(
            fullScreenFeature,
            findInPageIntegration,
            toolbarIntegration,
            sessionFeature
    )

    private val activityResultHandler: List<ViewBoundFeatureWrapper<*>> = listOf(
            promptsFeature
    )

    protected val sessionId: String?
        get() = arguments?.getString(SESSION_ID)

    protected var webAppToolbarShouldBeVisible = true

    final override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_browser, container, false)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        sessionFeature.set(
                feature = SessionFeature(
                        requireComponents.core.store,
                        requireComponents.useCases.sessionUseCases.goBack,
                        engineView,
                        sessionId),
                owner = this,
                view = view)

        (toolbar.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
            behavior = BrowserToolbarBehavior(
                    view.context,
                    null,
                    MozacToolbarBehaviorToolbarPosition.BOTTOM
            )
        }
        toolbarIntegration.set(
                feature = ToolbarIntegration(
                        requireContext(),
                        toolbar,
                        requireComponents.core.historyStorage,
                        requireComponents.core.sessionManager,
                        requireComponents.core.store,
                        requireComponents.useCases.sessionUseCases,
                        requireComponents.useCases.tabsUseCases,
                        sessionId,
                        activity?.supportFragmentManager
                ),
                owner = this,
                view = view)

        contextMenuIntegration.set(
                feature = ContextMenuIntegration(
                        requireContext(),
                        parentFragmentManager,
                        requireComponents.core.store,
                        requireComponents.useCases.tabsUseCases,
                        requireComponents.useCases.contextMenuUseCases,
                        view,
                        sessionId),
                owner = this,
                view = view)

        downloadsFeature.set(
                feature = DownloadsFeature(
                        requireContext(),
                        store = requireComponents.core.store,
                        useCases = requireComponents.useCases.downloadsUseCases,
                        fragmentManager = childFragmentManager,
                        downloadManager = FetchDownloadManager(
                                requireContext().applicationContext,
                                requireComponents.core.store,
                                DownloadService::class
                        ),
                        onNeedToRequestPermissions = { permissions ->
                            requestPermissions(permissions, REQUEST_CODE_DOWNLOAD_PERMISSIONS)
                        }),
                owner = this,
                view = view)

        promptsFeature.set(
                feature = PromptFeature(
                        fragment = this,
                        store = requireComponents.core.store,
                        customTabId = sessionId,
                        fragmentManager = parentFragmentManager,
                        onNeedToRequestPermissions = { permissions ->
                            requestPermissions(permissions, REQUEST_CODE_PROMPT_PERMISSIONS)
                        }),
                owner = this,
                view = view)

        windowFeature.set(
                feature = WindowFeature(requireComponents.core.store, requireComponents.useCases.tabsUseCases),
                owner = this,
                view = view
        )

        fullScreenFeature.set(
                feature = FullScreenFeature(
                        store = requireComponents.core.store,
                        sessionUseCases = requireComponents.useCases.sessionUseCases,
                        tabId = sessionId,
                        viewportFitChanged = ::viewportFitChanged,
                        fullScreenChanged = ::fullScreenChanged
                ),
                owner = this,
                view = view)

        findInPageIntegration.set(
                feature = FindInPageIntegration(
                        requireComponents.core.store,
                        sessionId,
                        findInPageBar as FindInPageView,
                        engineView),
                owner = this,
                view = view)

        sitePermissionFeature.set(
                feature = SitePermissionsFeature(
                        context = requireContext(),
                        fragmentManager = parentFragmentManager,
                        sessionId = sessionId,
                        storage = requireComponents.core.sitePermissionsStorage,
                        onNeedToRequestPermissions = { permissions ->
                            requestPermissions(permissions, REQUEST_CODE_APP_PERMISSIONS)
                        },
                        onShouldShowRequestPermissionRationale = { shouldShowRequestPermissionRationale(it) },
                        store = requireComponents.core.store),
                owner = this,
                view = view
        )

        (swipeRefresh.layoutParams as? CoordinatorLayout.LayoutParams)?.apply {
            behavior = EngineViewBrowserToolbarBehavior(
                    context,
                    null,
                    swipeRefresh,
                    toolbar.height,
                    MozacEngineBehaviorToolbarPosition.BOTTOM
            )
        }

        swipeRefreshFeature.set(
                feature = SwipeRefreshFeature(
                        requireComponents.core.store,
                        requireComponents.useCases.sessionUseCases.reload,
                        view.swipeRefresh
                ),
                owner = this,
                view = view
        )
    }

    private fun fullScreenChanged(enabled: Boolean) {
        if (enabled) {
            activity?.enterToImmersiveMode()
            toolbar.visibility = View.GONE
            engineView.setDynamicToolbarMaxHeight(0)
        } else {
            activity?.exitImmersiveModeIfNeeded()
            toolbar.visibility = View.VISIBLE
            engineView.setDynamicToolbarMaxHeight(resources.getDimensionPixelSize(R.dimen.browser_toolbar_height))
        }
    }

    private fun viewportFitChanged(viewportFit: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requireActivity().window.attributes.layoutInDisplayCutoutMode = viewportFit
        }
    }

    @CallSuper
    override fun onBackPressed(): Boolean {
        return backButtonHandler.any { it.onBackPressed() }
    }

    final override fun onHomePressed(): Boolean {
        return false
    }

    final override fun onPictureInPictureModeChanged(enabled: Boolean) {
        val session = requireComponents.core.store.state.selectedTab
        val fullScreenMode = session?.content?.fullScreen ?: false
        // If we're exiting PIP mode and we're in fullscreen mode, then we should exit fullscreen mode as well.
        if (!enabled && fullScreenMode) {
            onBackPressed()
            fullScreenChanged(false)
        }
    }

    final override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        val feature: PermissionsFeature? = when (requestCode) {
            REQUEST_CODE_DOWNLOAD_PERMISSIONS -> downloadsFeature.get()
            REQUEST_CODE_PROMPT_PERMISSIONS -> promptsFeature.get()
            REQUEST_CODE_APP_PERMISSIONS -> sitePermissionFeature.get()
            else -> null
        }
        feature?.onPermissionsResult(permissions, grantResults)
    }

    companion object {
        private const val SESSION_ID = "session_id"

        @JvmStatic
        protected fun Bundle.putSessionId(sessionId: String?) {
            putString(SESSION_ID, sessionId)
        }
    }

    final override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Logger.info("Fragment onActivityResult received with " +
                "requestCode: $requestCode, resultCode: $resultCode, data: $data")

        activityResultHandler.any { it.onActivityResult(requestCode, data, resultCode) }
    }
}
