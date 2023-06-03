package com.example.wavemap.ui.settings

import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.preference.*
import com.example.wavemap.R
import com.example.wavemap.dialogs.OpenSettingsDialog
import com.example.wavemap.services.BackgroundScanService
import com.example.wavemap.utilities.Misc
import com.example.wavemap.utilities.Permissions

class MainSettingsFragment : PreferenceFragmentCompat() {
    private val check_notification_permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (!Permissions.notification.all{ permission -> ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED }) {
            OpenSettingsDialog.Companion.App(R.string.missing_notification_permission, R.string.missing_notification_permission_desc)
                .show(parentFragmentManager, OpenSettingsDialog.TAG)
        } else {
            BackgroundScanService.start(requireActivity())
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)

        val periodic_scan_interval = preferenceManager.findPreference<EditTextPreference>("periodic_scan_interval")
        periodic_scan_interval?.setOnBindEditTextListener { edit_text -> edit_text.inputType = InputType.TYPE_CLASS_NUMBER }
        periodic_scan_interval?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text }s" }


        val background_scan = preferenceManager.findPreference<CheckBoxPreference>("background_scan")
        background_scan?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, enable ->
            if (enable as Boolean) {
                if (!checkAndPromptServicePermissions()) {
                    return@OnPreferenceChangeListener false
                } else {
                    BackgroundScanService.start(requireActivity())
                }
            }
            return@OnPreferenceChangeListener true
        }

        val notify_uncovered_area = preferenceManager.findPreference<CheckBoxPreference>("notify_uncovered_area")
        notify_uncovered_area?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, enable ->
            if (enable as Boolean) {
                if (checkAndPromptServicePermissions()) {
                    check_notification_permission.launch(Permissions.notification)
                }
                return@OnPreferenceChangeListener Permissions.notification.all{ permission -> ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED }
            }
            return@OnPreferenceChangeListener true
        }
    }

    private fun checkAndPromptServicePermissions() : Boolean {
        // Handle background location permission
        if (!Permissions.check(requireContext(), Permissions.background_gps)) {
            OpenSettingsDialog.Companion.App(R.string.missing_background_gps_permission, R.string.missing_background_gps_permission_desc)
                .show(parentFragmentManager, OpenSettingsDialog.TAG)
            return false
        }

        // Handle battery optimization (optional)
        if (Misc.isBatteryOptimizationOn(requireContext())) {
            OpenSettingsDialog.Companion.App(R.string.battery_optimization_permission, R.string.battery_optimization_permission_desc)
                .show(parentFragmentManager, OpenSettingsDialog.TAG)
        }

        return true
    }
}
