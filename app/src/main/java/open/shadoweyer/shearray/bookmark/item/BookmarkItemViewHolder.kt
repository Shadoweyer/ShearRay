package open.shadoweyer.shearray.bookmark.item

import android.content.Context
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.menu2.BrowserMenuController
import mozilla.components.concept.menu.MenuController
import mozilla.components.concept.menu.Orientation
import mozilla.components.concept.menu.candidate.TextMenuCandidate
import mozilla.components.concept.menu.candidate.TextStyle
import mozilla.components.concept.storage.BookmarkNode
import mozilla.components.concept.storage.BookmarkNodeType
import open.shadoweyer.shearray.R
import open.shadoweyer.shearray.bookmark.BookmarkMediator

class BookmarkItemViewHolder(
        private val context: Context,
        private val containerView: View,
        private val m: BookmarkMediator
) : RecyclerView.ViewHolder(containerView) {

    var item: BookmarkNode? = null

    private val titleView: TextView get() = containerView.findViewById(R.id.title)

    private val urlView: TextView get() = containerView.findViewById(R.id.url)

    val iconView: ImageView get() = containerView.findViewById(R.id.favicon)

    private val overflowView: ImageButton get() = containerView.findViewById(R.id.overflow_menu)

    private val menuController: MenuController by lazy { BrowserMenuController() }

    init {
        overflowView.setOnClickListener {
            menuController.show(
                    anchor = it,
                    orientation = Orientation.DOWN
            )
        }
        containerView.setOnClickListener {
            m.open(item)
        }

    }

    fun bind(item: BookmarkNode) {
        this.item = item

        menuController.submitList(menuItems(item.type))

        urlView.isVisible = item.type == BookmarkNodeType.ITEM

        val useTitleFallback = item.type == BookmarkNodeType.ITEM && item.title.isNullOrBlank()
        titleView.text = if (useTitleFallback) item.url else item.title

        urlView.text = item.url

        updateIcon(item)
    }

    private fun menuItems(itemType: BookmarkNodeType): List<TextMenuCandidate> {
        return listOfNotNull(
                if (itemType != BookmarkNodeType.SEPARATOR) {
                    TextMenuCandidate(
                            text = context.getString(R.string.bookmark_menu_edit_button)
                    ) {
                        item?.let {
                            m.edit(it) {
//                                Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    null
                },
                if (itemType == BookmarkNodeType.ITEM) {
                    TextMenuCandidate(
                            text = context.getString(R.string.bookmark_menu_open_in_new_tab_button)
                    ) {
                        m.openNew(item)
                    }
                } else {
                    null
                },
                if (itemType == BookmarkNodeType.ITEM) {
                    TextMenuCandidate(
                            text = context.getString(R.string.bookmark_menu_open_in_private_tab_button)
                    ) {
                        m.openNewPrivate(item)
                    }
                } else {
                    null
                },
                TextMenuCandidate(
                        text = context.getString(R.string.bookmark_menu_delete_button),
                        textStyle = TextStyle(color = ContextCompat.getColor(context, R.color.photonRed50))
                ) {
                    m.delete(item)
                }
        )
    }

    private fun updateIcon(item: BookmarkNode) {
//        val context = containerView.context
//        val iconView = containerView.iconView
//        val url = item.url
//
//        when {
//            // Item is a folder
//            item.type == BookmarkNodeType.FOLDER ->
//                iconView.setImageDrawable(
//                        context.getDrawableWithTint(
//                                R.drawable.ic_folder_icon,
//                                ContextCompat.getColor(context, R.color.primary_text_light_theme)
//                        )
//                )
//            // Item has a http/https URL
//            url != null && url.startsWith("http") ->
//                context.components.core.icons.loadIntoView(iconView, url)
//            else ->
//                iconView.setImageDrawable(null)
//        }
    }
}