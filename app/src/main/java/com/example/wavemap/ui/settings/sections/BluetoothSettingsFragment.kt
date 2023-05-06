package com.example.wavemap.ui.settings.sections

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.example.wavemap.R

class BluetoothSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.bluetooth_preferences, rootKey)
    }
}
