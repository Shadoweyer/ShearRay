package open.shadoweyer.shearray.bookmark

import android.content.Context
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import open.shadoweyer.shearray.R
import open.shadoweyer.shearray.ext.components

class BookmarkMediator(private val context: Context) {

    var adapter: BookmarkAdapter? = null
    var frag: BookmarkFragment? = null
    private val storage: LocalBookmarksStorage = LocalBookmarksStorage(context)
    private val mScope = MainScope()

    fun update() {
        mScope.launch {
            var testNodes = storage.getBookmarkByParent(LocalBookmarksStorage.BOOKMARK_ROOT_ID)
            adapter?.updateData(testNodes)
        }
    }

//    fun test() {
//        var testNodes = listOf(
//                BookmarkNode(BookmarkNodeType.ITEM, "a item", LocalBookmarksStorage.BOOKMARK_ROOT_ID, null, "A item", "same url", null),
//                BookmarkNode(BookmarkNodeType.FOLDER, "a folder", LocalBookmarksStorage.BOOKMARK_ROOT_ID, null, "A folder", null, null),
//                BookmarkNode(BookmarkNodeType.ITEM, "a sub item", "a folder", null, "A sub item", "same other url", null),
//        )
//        adapter?.updateData(testNodes)
//    }

    fun add(url: String, title: String, callback: suspend () -> Unit) {
        mScope.launch {
            storage.addItem(LocalBookmarksStorage.BOOKMARK_ROOT_ID, url, title, null)
            callback()
        }
    }

    fun edit(item: BookmarkNode, callback: suspend () -> Unit) {
        frag?.parentFragmentManager?.beginTransaction()
                ?.replace(R.id.container, BookmarkEditFragment(item) {
                    mScope.launch {
                        storage.updateNode(item.guid, it)
                        val testNodes = storage.getBookmarkByParent(LocalBookmarksStorage.BOOKMARK_ROOT_ID)
                        adapter?.updateData(testNodes)
                        callback()
                    }
                })
                ?.addToBackStack(null)
                ?.commit()
    }

    fun open(item: BookmarkNode?) {
        if (item?.type == BookmarkNodeType.ITEM) {
            context.components.useCases.sessionUseCases.loadUrl(item.url ?: "")
            frag?.parentFragmentManager?.popBackStack()
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
            adapter?.updateData(testNodes)
        }
    }

    fun warmUp() {
        mScope.launch {
            storage.warmUp()
        }
    }

}