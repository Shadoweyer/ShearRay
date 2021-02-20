/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package open.shadoweyer.shearray

import android.content.Context
import open.shadoweyer.shearray.bookmark.BookmarkMediator
import open.shadoweyer.shearray.components.*

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
            core.store
        )
    }

    val bookmarkMediator by lazy { BookmarkMediator(context) }

}
