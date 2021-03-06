package open.shadoweyer.shearray.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_sectting_container.*
import mozilla.components.support.base.feature.UserInteractionHandler
import open.shadoweyer.shearray.R


class SettingContainerFragment : Fragment(), UserInteractionHandler {

    private val layoutID = R.layout.fragment_sectting_container
    override fun onResume() {
        super.onResume()
        setting_back.setOnClickListener {
            if (!onBackPressed()) {
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutID, container, false)
    }

    fun updateTitle(title: String) {
        textView.text = title
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