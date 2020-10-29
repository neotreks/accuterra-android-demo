package com.neotreks.accuterra.mobile.demo.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.neotreks.accuterra.mobile.demo.R

/**
 * User Settings Fragment
 */
class UserSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}