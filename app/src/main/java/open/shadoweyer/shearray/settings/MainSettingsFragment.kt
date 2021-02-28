/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package open.shadoweyer.shearray.settings

import android.os.Bundle
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import open.shadoweyer.shearray.R
import open.shadoweyer.shearray.R.string.*
import open.shadoweyer.shearray.ext.getPreferenceKey
import open.shadoweyer.shearray.ext.requireComponents

@Suppress("TooManyFunctions")
class MainSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onResume() {
        super.onResume()
        setupPreferences()
        (parentFragment as SettingContainerFragment).updateTitle(getString(R.string.setting_main_title))
    }

    @Suppress("LongMethod") // Yep, this should be refactored.
    private fun setupPreferences() {
        val remoteDebuggingKey = context?.getPreferenceKey(pref_key_remote_debugging)
        val aboutPageKey = context?.getPreferenceKey(pref_key_about_page)
        val privacyKey = context?.getPreferenceKey(pref_key_privacy)

        val preferenceRemoteDebugging = findPreference(remoteDebuggingKey)
        val preferenceAboutPage = findPreference(aboutPageKey)
        val preferencePrivacy = findPreference(privacyKey)

        preferenceRemoteDebugging.onPreferenceChangeListener = getChangeListenerForRemoteDebugging()
        preferenceAboutPage.onPreferenceClickListener = getAboutPageListener()
        preferencePrivacy.onPreferenceClickListener = getClickListenerForPrivacy()

    }

    private fun getClickListenerForPrivacy(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                    .replace(R.id.setting_root, PrivacySettingsFragment())
                    .addToBackStack(null)
                    .commit()
            true
        }
    }

    private fun getChangeListenerForRemoteDebugging(): OnPreferenceChangeListener {
        return OnPreferenceChangeListener { _, newValue ->
            requireComponents.core.engine.settings.remoteDebuggingEnabled = newValue as Boolean
            true
        }
    }

    private fun getAboutPageListener(): OnPreferenceClickListener {
        return OnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                    .replace(R.id.setting_root, AboutFragment())
                    .addToBackStack(null)
                    .commit()
            true
        }
    }
}
