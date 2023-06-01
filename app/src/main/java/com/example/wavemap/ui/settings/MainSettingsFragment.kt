package com.example.wavemap.ui.settings

import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import androidx.core.content.ContextCompat
import androidx.preference.*
import com.example.wavemap.R
import com.example.wavemap.dialogs.BackgroundServicePermissionsDialog
import com.example.wavemap.dialogs.BatteryOptimizationDialog
import com.example.wavemap.services.BackgroundScanService
import com.example.wavemap.utilities.Misc
import com.example.wavemap.utilities.Permissions

class MainSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)

        val periodic_scan_interval = preferenceManager.findPreference<EditTextPreference>("periodic_scan_interval")
        periodic_scan_interval?.setOnBindEditTextListener { edit_text -> edit_text.inputType = InputType.TYPE_CLASS_NUMBER }
        periodic_scan_interval?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text }s" }

        // Listener for settings that may require to start the BackgroundScanService
        val service_options_listener = Preference.OnPreferenceChangeListener { preference, enable ->
            if (enable as Boolean) {
                // Handle background location permission and battery optimization
                if (!Permissions.background_gps.all{ perm -> ContextCompat.checkSelfPermission(requireContext(), perm) == PackageManager.PERMISSION_GRANTED }) {
                    BackgroundServicePermissionsDialog {
                        BackgroundScanService.start(requireActivity())
                    }.show(childFragmentManager, BackgroundServicePermissionsDialog.TAG)
                } else if (Misc.isBatteryOptimizationOn(requireContext())) {
                    BatteryOptimizationDialog{
                        BackgroundScanService.start(requireActivity())
                    }.show(childFragmentManager, BatteryOptimizationDialog.TAG)
                } else {
                    BackgroundScanService.start(requireActivity())
                }
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
