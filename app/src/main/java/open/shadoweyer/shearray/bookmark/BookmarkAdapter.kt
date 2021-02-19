package open.shadoweyer.shearray.bookmark

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import open.shadoweyer.shearray.R
import open.shadoweyer.shearray.bookmark.item.BookmarkItemViewHolder
import open.shadoweyer.shearray.bookmark.item.BookmarkSeparatorViewHolder

class BookmarkAdapter(private val m: BookmarkMediator) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var currentFolderNodes = mutableListOf<BookmarkNode>()

    init {
        m.adapter = this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)

        return when (viewType) {
            ViewHolderType_Default ->
                BookmarkItemViewHolder(parent.context, view, m)
            ViewHolderType_Seperator ->
                BookmarkSeparatorViewHolder(view)
            else -> throw IllegalStateException("ViewType $viewType does not match to a ViewHolder")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? BookmarkItemViewHolder)?.apply {
            bind(currentFolderNodes[position])
        }
    }

    override fun getItemViewType(position: Int) = when (currentFolderNodes[position].type) {
        BookmarkNodeType.ITEM, BookmarkNodeType.FOLDER -> ViewHolderType_Default
        BookmarkNodeType.SEPARATOR -> ViewHolderType_Seperator
    }

    override fun getItemCount(): Int {
        return currentFolderNodes.size
    }

    companion object {
        private const val ViewHolderType_Default = R.layout.bookmark_item
        private const val ViewHolderType_Seperator = R.layout.bookmark_separator
    }

    fun updateData(allNodes: List<BookmarkNode>) {

        val folders: MutableList<BookmarkNode> = mutableListOf()
        val notFolders: MutableList<BookmarkNode> = mutableListOf()
        allNodes.forEach {
            if (it.type == BookmarkNodeType.FOLDER) {
                folders.add(it)
            } else {
                notFolders.add(it)
            }
        }
        val newTree = folders + notFolders

        val diffUtil = DiffUtil.calculateDiff(
                BookmarkDiffUtil(
                        this.currentFolderNodes,
                        newTree
                )
        )

        this.currentFolderNodes = newTree as MutableList<BookmarkNode>
        diffUtil.dispatchUpdatesTo(this)
    }

    @VisibleForTesting
    internal class BookmarkDiffUtil(
            val old: List<BookmarkNode>,
            val new: List<BookmarkNode>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                old[oldItemPosition].guid == new[newItemPosition].guid

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                old[oldItemPosition] == new[newItemPosition]

        override fun getOldListSize(): Int = old.size
        override fun getNewListSize(): Int = new.size
    }
}