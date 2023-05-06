package com.example.wavemap.ui.settings.sections

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.wavemap.R

class NoiseSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.noise_preferences, rootKey)
    }
}
