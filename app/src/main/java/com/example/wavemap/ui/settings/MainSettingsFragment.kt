package com.example.wavemap.ui.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.wavemap.R

class MainSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)
    }
}
