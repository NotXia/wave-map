package com.example.wavemap.ui.settings.sections

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.wavemap.R

class WiFiSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.wifi_preferences, rootKey)
    }
}
