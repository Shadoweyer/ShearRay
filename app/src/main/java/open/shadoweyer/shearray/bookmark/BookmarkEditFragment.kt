package open.shadoweyer.shearray.bookmark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_bookmark.*
import kotlinx.android.synthetic.main.fragment_bookmark_edit.*
import mozilla.components.concept.storage.BookmarkInfo
import mozilla.components.concept.storage.BookmarkNode
import open.shadoweyer.shearray.R
import open.shadoweyer.shearray.ext.components

class BookmarkEditFragment(
        private val node: BookmarkNode
) : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_bookmark_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bookmark_edit_title.setText(node.title ?: "")
        bookmark_edit_url.setText(node.url ?: "")
        bookmark_edit_confirm.setOnClickListener {
            val title=bookmark_edit_title.text.toString()
            val url=bookmark_edit_url.text.toString()
            if (title != node.title || url != node.url) {
                requireContext().components.bookmarkMediator.updateItem(node.guid,BookmarkInfo(null, null, title, url))
                Snackbar.make(requireView(),"Updated",2000).show()
            }
            parentFragmentManager.popBackStack()
        }
        bookmark_edit_back.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

    }
}