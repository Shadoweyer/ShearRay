package open.shadoweyer.shearray.bookmark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_bookmark.view.*
import open.shadoweyer.shearray.R
import open.shadoweyer.shearray.ext.components

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BookmarkFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BookmarkFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var listView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().components.bookmarkMediator.update()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_bookmark, container, false)
        listView = view.bookmark_list
        val m = requireContext().components.bookmarkMediator
        m.frag = this
        listView.adapter = BookmarkAdapter(m)
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listView.adapter = null
        requireContext().components.bookmarkMediator.adapter = null
        requireContext().components.bookmarkMediator.frag = null
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                BookmarkFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}