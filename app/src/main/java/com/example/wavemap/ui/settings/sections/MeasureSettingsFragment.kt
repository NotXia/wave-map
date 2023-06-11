package com.example.wavemap.ui.settings.sections

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.wavemap.R
import com.example.wavemap.ui.settings.edittexts.EditFloatPreference
import com.example.wavemap.ui.settings.edittexts.EditUnsignedIntPreference
import com.example.wavemap.utilities.Constants


open class MeasureSettingsFragment(
    private val label_id: Int,
    private val key: String,
    private val measure_unit: String,
    private val range_bad_default: Double,
    private val range_good_default: Double,
    private val past_limit_default: Int,
    private val range_size_default: Int
) : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.measure_preferences, rootKey)

        // Setting default values (if needed)
        val pref_manager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor: SharedPreferences.Editor = pref_manager.edit()

        if (!pref_manager.contains("${key}_range_bad"))     { editor.putFloat("${key}_range_bad", range_bad_default.toFloat()) }
        if (!pref_manager.contains("${key}_range_good"))    { editor.putFloat("${key}_range_good", range_good_default.toFloat()) }
        if (!pref_manager.contains("${key}_past_limit"))     { editor.putInt("${key}_past_limit", past_limit_default) }
        if (!pref_manager.contains("${key}_range_size"))     { editor.putInt("${key}_range_size", range_size_default) }
        editor.commit()

        // Creating settings elements
        val category = PreferenceCategory(preferenceScreen.context)
        category.title = getString(label_id)

        val pref_range_bad = EditFloatPreference(preferenceScreen.context)
        pref_range_bad.key = "${key}_range_bad"
        pref_range_bad.title = getString(R.string.range_bad)
        pref_range_bad.dialogTitle = getString(R.string.range_bad)
        pref_range_bad.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text ?: range_bad_default } $measure_unit" }

        val pref_range_good = EditFloatPreference(preferenceScreen.context)
        pref_range_good.key = "${key}_range_good"
        pref_range_good.title = getString(R.string.range_good)
        pref_range_good.dialogTitle = getString(R.string.range_good)
        pref_range_good.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text ?: range_good_default } $measure_unit" }

        val pref_past_limit = EditUnsignedIntPreference(preferenceScreen.context)
        pref_past_limit.key = "${key}_past_limit"
        pref_past_limit.title = getString(R.string.past_limit)
        pref_past_limit.dialogTitle = getString(R.string.past_limit)
        pref_past_limit.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text ?: past_limit_default }" }
        pref_past_limit.setOnPreferenceChangeListener { _, new_value ->
            val new_past_limit = (new_value as String).toInt()
            return@setOnPreferenceChangeListener new_past_limit >= 0
        }

        val pref_range_size = EditUnsignedIntPreference(preferenceScreen.context)
        pref_range_size.key = "${key}_range_size"
        pref_range_size.title = getString(R.string.range_size)
        pref_range_size.dialogTitle = "${getString(R.string.range_size)} (1 - ${Constants.HUE_MEASURE_RANGE.second.toInt()})"
        pref_range_size.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text ?: range_size_default }" }
        pref_range_size.setOnPreferenceChangeListener { _, new_value ->
            val new_range_size = (new_value as String).toInt()
            return@setOnPreferenceChangeListener 1 <= new_range_size && new_range_size <= Constants.HUE_MEASURE_RANGE.second.toInt()
        }

        preferenceScreen.addPreference(category)
        category.addPreference(pref_range_bad)
        category.addPreference(pref_range_good)
        category.addPreference(pref_past_limit)
        category.addPreference(pref_range_size)
    }

}
