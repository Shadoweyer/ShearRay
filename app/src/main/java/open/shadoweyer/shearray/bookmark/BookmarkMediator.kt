package open.shadoweyer.shearray.bookmark

import android.content.Context
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.concept.storage.BookmarkInfo
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import open.shadoweyer.shearray.BrowserActivity
import open.shadoweyer.shearray.R
import open.shadoweyer.shearray.ext.components
import java.io.Reader
import java.io.Writer

class BookmarkMediator(private val context: Context) {

    var frag: BookmarkFragment? = null
    private val storage: LocalBookmarksStorage = LocalBookmarksStorage(context)
    private val mScope = MainScope()

    //State vars
    private val parentNodeStack = mutableListOf<BookmarkNode>()

    fun warmUp() {
        mScope.launch {
            storage.warmUp()
        }
    }

    /* region Bookmark IO functions */
    fun importBookmarkFromExternalIntent(r: Reader, activity: BrowserActivity) {
        mScope.launch {
            if (storage.import(r)) {
                activity.notifyImport()
            }
        }
    }

    fun export(w: Writer) {
        mScope.launch {
            if (storage.export(w)) {
                frag?.notifyExport()
            }
        }
    }
    /* endregion name */

    /* region Bookmark item menu calls */
    fun edit(item: BookmarkNode, callback: suspend () -> Unit) {
        frag?.parentFragmentManager?.beginTransaction()
                ?.replace(R.id.top_container, BookmarkEditFragment(item))
                ?.addToBackStack(null)
                ?.commit()
    }

    fun openNew(item: BookmarkNode?) {
        if (item?.type == BookmarkNodeType.ITEM) {
            context.components.useCases.tabsUseCases.addTab(item.url ?: "")
            frag?.parentFragmentManager?.popBackStack()
        }
    }

    fun openNewPrivate(item: BookmarkNode?) {
        if (item?.type == BookmarkNodeType.ITEM) {
            context.components.useCases.tabsUseCases.addPrivateTab(item.url ?: "")
            frag?.parentFragmentManager?.popBackStack()
        }
    }

    fun delete(item: BookmarkNode?) {
        if (item == null) return
        mScope.launch {
            var rst = storage.deleteNode(item.guid)
            val testNodes = storage.getBookmarkByParent(LocalBookmarksStorage.BOOKMARK_ROOT_ID)
            frag?.updateList(testNodes)
        }
    }
    /* endregion */

    /* region Back button handler */
    fun handleBackPressed(): Boolean {
        return if (parentNodeStack.size == 0) false
        else {
            parentNodeStack.removeLast()
            update()
            true
        }
    }

    /* endregion */
    fun update() {
        val parentNode = parentNodeStack.lastOrNull()
        val parentID = parentNode?.guid ?: LocalBookmarksStorage.BOOKMARK_ROOT_ID
        val parentTitle = parentNode?.title ?: context.getString(R.string.bookmark_title)
        frag?.updateTitle(parentTitle)
        mScope.launch {
            val resultNodes = storage.getBookmarkByParent(parentID)
            frag?.updateList(resultNodes)
        }
    }

    fun add(url: String, title: String, callback: suspend () -> Unit) {
        mScope.launch {
            storage.addItem(LocalBookmarksStorage.BOOKMARK_ROOT_ID, url, title, null)
            callback()
        }
    }


    fun open(item: BookmarkNode?) {
        if (item != null) {
            if (item.type == BookmarkNodeType.ITEM) {
                context.components.useCases.sessionUseCases.loadUrl(item.url ?: "")
                frag?.parentFragmentManager?.popBackStack()
            } else if (item.type == BookmarkNodeType.FOLDER) {
                parentNodeStack.add(item)
                update()
            }
        }
    }


    fun updateItem(itemId: String, info: BookmarkInfo) {
        mScope.launch {
            storage.updateNode(itemId, info)
        }
    }


}