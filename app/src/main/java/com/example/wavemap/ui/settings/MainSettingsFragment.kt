package com.example.wavemap.ui.settings

import android.os.Bundle
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.CheckBoxPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.wavemap.R
import com.example.wavemap.services.BackgroundScanService

class MainSettingsFragment : PreferenceFragmentCompat() {
    private val permission_check_and_service_start = registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions() ) { permissions ->
        BackgroundScanService.start(requireActivity())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)

        val periodic_scan_interval = preferenceManager.findPreference<EditTextPreference>("periodic_scan_interval")
        periodic_scan_interval?.setOnBindEditTextListener { edit_text -> edit_text.inputType = InputType.TYPE_CLASS_NUMBER }
        periodic_scan_interval?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text }s" }

        // Listener for settings that may require to start the BackgroundScanService
        val service_options_listener = Preference.OnPreferenceChangeListener { preference, enable ->
            if (enable as Boolean) {
                permission_check_and_service_start.launch(arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            }
            else {
                if (!BackgroundScanService.needToStartService(requireActivity())) { BackgroundScanService.stop(requireActivity()) }
            }
            return@OnPreferenceChangeListener true
        }

        // Background scan preference and service start
        val background_scan = preferenceManager.findPreference<CheckBoxPreference>("background_scan")
        background_scan?.onPreferenceChangeListener = service_options_listener

        // Background scan preference and service start
        val notify_uncovered_area = preferenceManager.findPreference<CheckBoxPreference>("notify_uncovered_area")
        notify_uncovered_area?.onPreferenceChangeListener = service_options_listener
    }
}
