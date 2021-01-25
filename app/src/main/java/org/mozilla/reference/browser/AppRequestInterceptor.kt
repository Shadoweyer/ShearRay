/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser

import android.content.Context
import mozilla.components.browser.errorpages.ErrorPages
import mozilla.components.browser.errorpages.ErrorType
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.request.RequestInterceptor
import org.mozilla.reference.browser.ext.components
import org.mozilla.reference.browser.tabs.PrivatePage

class AppRequestInterceptor(private val context: Context) : RequestInterceptor {
    override fun onLoadRequest(
        engineSession: EngineSession,
        uri: String,
        lastUri: String?,
        hasUserGesture: Boolean,
        isSameDomain: Boolean,
        isRedirect: Boolean,
        isDirectNavigation: Boolean,
        isSubframeRequest: Boolean
    ): RequestInterceptor.InterceptionResponse? {
        return when (uri) {
            "about:privatebrowsing" -> {
                val page = PrivatePage.createPrivateBrowsingPage(context, uri)
                RequestInterceptor.InterceptionResponse.Content(page, encoding = "base64")
            }

            else -> {
                context.components.services.appLinksInterceptor.onLoadRequest(
                    engineSession, uri, lastUri, hasUserGesture, isSameDomain, isRedirect, isDirectNavigation,
                    isSubframeRequest
                )
            }
        }
    }

    override fun onErrorRequest(
        session: EngineSession,
        errorType: ErrorType,
        uri: String?
    ): RequestInterceptor.ErrorResponse? {
        val errorPage = ErrorPages.createUrlEncodedErrorPage(context, errorType, uri)
        return RequestInterceptor.ErrorResponse(errorPage)
    }

    override fun interceptsAppInitiatedRequests() = true
}
