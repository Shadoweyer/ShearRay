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

    fun importBookmarkFromExternalIntent(r:Reader,activity: BrowserActivity) {
        mScope.launch {
            if(storage.import(r)){
                activity.notifyImport()
            }
        }
    }

    fun export(w:Writer) {
        mScope.launch {
            if(storage.export(w)){
                frag?.notifyExport()
            }
        }
    }
    fun update() {
        mScope.launch {
            val testNodes = storage.getBookmarkByParent(LocalBookmarksStorage.BOOKMARK_ROOT_ID)
            frag?.updateList(testNodes)
        }
    }

    fun add(url: String, title: String, callback: suspend () -> Unit) {
        mScope.launch {
            storage.addItem(LocalBookmarksStorage.BOOKMARK_ROOT_ID, url, title, null)
            callback()
        }
    }

    fun edit(item: BookmarkNode, callback: suspend () -> Unit) {
        frag?.parentFragmentManager?.beginTransaction()
                ?.replace(R.id.top_container, BookmarkEditFragment(item))
                ?.addToBackStack(null)
                ?.commit()
    }

    fun open(item: BookmarkNode?) {
        if (item != null) {
            if (item.type == BookmarkNodeType.ITEM) {
                context.components.useCases.sessionUseCases.loadUrl(item.url ?: "")
                frag?.parentFragmentManager?.popBackStack()
            }else if (item.type == BookmarkNodeType.FOLDER) {
                context.components.useCases.sessionUseCases.loadUrl(item.url ?: "")
                frag?.parentFragmentManager?.popBackStack()
            }
        }
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

    fun updateItem(itemId:String,info: BookmarkInfo) {
        mScope.launch {
            storage.updateNode(itemId, info)
        }
    }

    fun warmUp() {
        mScope.launch {
            storage.warmUp()
        }
    }

}