package open.shadoweyer.shearray.settings

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_sectting_container.*
import mozilla.components.support.base.feature.UserInteractionHandler
import open.shadoweyer.shearray.R

/**
 * A simple [Fragment] subclass.
 * Use the [SettingContainerFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingContainerFragment : Fragment(), UserInteractionHandler {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sectting_container, container, false)
    }
    fun updateTitle(title:String){
        textView.text = title
    }
    companion object {
    }

    override fun onBackPressed(): Boolean {
        if (childFragmentManager.backStackEntryCount == 1) {
            updateTitle(getString(R.string.setting_main_title))
        }
        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            return true
        }
        return false
    }
}