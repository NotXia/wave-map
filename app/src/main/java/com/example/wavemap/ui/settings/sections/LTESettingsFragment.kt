package com.example.wavemap.ui.settings.sections

import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.wavemap.R

class LTESettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.lte_preferences, rootKey)

        val range_bad = preferenceManager.findPreference<EditTextPreference>("lte_range_bad")
        val range_good = preferenceManager.findPreference<EditTextPreference>("lte_range_good")
        val past_limit = preferenceManager.findPreference<EditTextPreference>("lte_past_limit")

        range_bad?.setOnBindEditTextListener { edit_text -> edit_text.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED }
        range_bad?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text ?: getString(R.string.lte_default_range_bad) } dBm" }

        range_good?.setOnBindEditTextListener { edit_text -> edit_text.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED }
        range_good?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text ?: getString(R.string.lte_default_range_good) } dBm" }

        past_limit?.setOnBindEditTextListener { edit_text -> edit_text.inputType = InputType.TYPE_CLASS_NUMBER }
        past_limit?.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text ?: 1 }" }
    }
}
