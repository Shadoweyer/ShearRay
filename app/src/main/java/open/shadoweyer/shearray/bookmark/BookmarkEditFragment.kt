package open.shadoweyer.shearray.bookmark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_bookmark_edit.*
import mozilla.components.concept.storage.BookmarkInfo
import mozilla.components.concept.storage.BookmarkNode
import open.shadoweyer.shearray.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BookmarkEditFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BookmarkEditFragment(
        private val node: BookmarkNode,
        private val callback: ((BookmarkInfo) -> Unit)
) : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_bookmark_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bookmark_edit_title.setText(node.title ?: "")
        bookmark_edit_url.setText(node.url ?: "")
        bookmark_edit_confirm.setOnClickListener {
            if (bookmark_edit_title.text.toString() != node.title || bookmark_edit_url.text.toString() != node.url) {
                callback(BookmarkInfo(null, null, bookmark_edit_title.text.toString(),
                        bookmark_edit_url.text.toString()))
            }
            parentFragmentManager.popBackStack()
        }

    }
}


//companion object {
//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment BookmarkEditFragment.
//     */
// TODO: Rename and change types and number of parameters
//        @JvmStatic
//        fun newInstance(param1: String, param2: String) =
//                BookmarkEditFragment().apply {
//                    arguments = Bundle().apply {
//                        putString(ARG_PARAM1, param1)
//                        putString(ARG_PARAM2, param2)
//                    }
//                }
//}
//}