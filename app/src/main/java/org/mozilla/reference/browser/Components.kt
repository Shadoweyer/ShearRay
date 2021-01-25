/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.reference.browser

import android.content.Context
import org.mozilla.reference.browser.components.Core
import org.mozilla.reference.browser.components.Services
import org.mozilla.reference.browser.components.Search
import org.mozilla.reference.browser.components.Utilities
import org.mozilla.reference.browser.components.UseCases

/**
 * Provides access to all components.
 */
class Components(private val context: Context) {
    val core by lazy { Core(context) }
    val search by lazy { Search(context) }
    val useCases by lazy {
        UseCases(
            context,
            core.engine,
            core.sessionManager,
            core.store,
            core.shortcutManager
        )
    }

    val analytics by lazy {  }
    val utils by lazy {
        Utilities(
            context,
            core.store,
            useCases.sessionUseCases,
            useCases.searchUseCases,
            useCases.tabsUseCases,
            useCases.customTabsUseCases
        )
    }
    val services by lazy { Services(context, useCases.tabsUseCases) }
}
