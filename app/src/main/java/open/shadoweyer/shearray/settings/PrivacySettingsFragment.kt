/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package open.shadoweyer.shearray.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import open.shadoweyer.shearray.R
import open.shadoweyer.shearray.ext.getPreferenceKey
import open.shadoweyer.shearray.ext.requireComponents

class PrivacySettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.privacy_preferences, rootKey)

        val trackingProtectionNormalKey = context?.getPreferenceKey(R.string.pref_key_tracking_protection_normal)
        val trackingProtectionPrivateKey = context?.getPreferenceKey(R.string.pref_key_tracking_protection_private)

        val prefTrackingProtectionNormal = findPreference(trackingProtectionNormalKey)
        val prefTrackingProtectionPrivate = findPreference(trackingProtectionPrivateKey)

        prefTrackingProtectionNormal.setOnPreferenceChangeListener { preference, newValue ->
            val policy = requireComponents.core.createTrackingProtectionPolicy(normalMode = newValue as Boolean)
            requireComponents.useCases.settingsUseCases.updateTrackingProtection.invoke(policy)
            true
        }
        prefTrackingProtectionPrivate.setOnPreferenceChangeListener { preference, newValue ->
            val policy = requireComponents.core.createTrackingProtectionPolicy(privateMode = newValue as Boolean)
            requireComponents.useCases.settingsUseCases.updateTrackingProtection.invoke(policy)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        (parentFragment as SettingContainerFragment).updateTitle(getString(R.string.setting_main_privacy))

    }
}
