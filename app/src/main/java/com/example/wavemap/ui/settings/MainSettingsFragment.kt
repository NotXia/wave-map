package com.example.wavemap.ui.settings

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.wavemap.R

class MainSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)

        val periodic_scan_interval = preferenceManager.findPreference<EditTextPreference>("periodic_scan_interval")
        periodic_scan_interval?.setOnBindEditTextListener { edit_text -> edit_text.inputType = InputType.TYPE_CLASS_NUMBER }
        periodic_scan_interval?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text }s" }
    }
}
