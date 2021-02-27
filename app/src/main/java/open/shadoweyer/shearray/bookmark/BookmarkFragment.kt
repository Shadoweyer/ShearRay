package open.shadoweyer.shearray.bookmark

import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_bookmark.*
import mozilla.components.concept.storage.BookmarkNode
import open.shadoweyer.shearray.R
import open.shadoweyer.shearray.ext.components

class BookmarkFragment : Fragment() {
    private val layoutID=R.layout.fragment_bookmark

    private val fileReqID=1

    private lateinit var listView: RecyclerView
    private lateinit var adapter: BookmarkAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        val m = requireContext().components.bookmarkMediator
        m.frag = this
        adapter=BookmarkAdapter(m)
        listView.adapter = adapter
        m.update()
    }

    override fun onPause() {
        super.onPause()
        val m = requireContext().components.bookmarkMediator
        m.frag = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutID, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView = bookmark_list

        back.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        export.setOnClickListener {
            val createFileIntent=Intent(Intent.ACTION_CREATE_DOCUMENT)
            createFileIntent.type = "text/html"
            createFileIntent.addCategory(Intent.CATEGORY_OPENABLE)
            createFileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,false)
            createFileIntent.putExtra(Intent.EXTRA_TITLE,"Bookmark")

            startActivityForResult(createFileIntent,fileReqID)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==fileReqID){
            data?.data?.let {
                val writer=requireActivity().contentResolver.openOutputStream(it)?.writer()?.buffered()
                if (writer != null) {
                    requireContext().components.bookmarkMediator.export(writer)
                }
            }
        }
    }

    fun updateList(data:List<BookmarkNode>){
        adapter.updateData(data)
    }

    fun notifyExport() {
        Snackbar.make(requireView(),"Exported",2000).show()
    }
}