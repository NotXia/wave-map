package com.example.wavemap.ui.settings.sections

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.wavemap.R
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

        if (pref_manager.getString("${key}_range_bad", "") == "") { editor.putString("${key}_range_bad", "$range_bad_default") }
        if (pref_manager.getString("${key}_range_good", "") == "") { editor.putString("${key}_range_good", "$range_good_default") }
        if (pref_manager.getString("${key}_past_limit", "") == "") { editor.putString("${key}_past_limit", "$past_limit_default") }
        if (pref_manager.getString("${key}_range_size", "") == "") { editor.putString("${key}_range_size", "$range_size_default") }
        editor.commit()

        // Creating settings elements
        val category = PreferenceCategory(preferenceScreen.context)
        category.title = getString(label_id)
        preferenceScreen.addPreference(category)

        preferenceScreen.addPreference(createEditText(
            "${key}_range_bad", getString(R.string.range_bad),
            Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text ?: range_bad_default } ${measure_unit}" },
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        ))
        preferenceScreen.addPreference(createEditText(
            "${key}_range_good", getString(R.string.range_good),
            Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text ?: range_good_default } ${measure_unit}" },
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        ))
        val past_limit_edittext = createEditText(
            "${key}_past_limit", getString(R.string.past_limit),
            Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text ?: past_limit_default }" },
            InputType.TYPE_CLASS_NUMBER
        )
        past_limit_edittext.setOnPreferenceChangeListener { _, new_value ->
            val new_past_limit = (new_value as String).toInt()
            return@setOnPreferenceChangeListener new_past_limit >= 0
        }
        preferenceScreen.addPreference(past_limit_edittext)

        val range_size_edittext = createEditText(
            "${key}_range_size", getString(R.string.range_size),
            Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text ?: range_size_default }" },
            InputType.TYPE_CLASS_NUMBER
        )
        range_size_edittext.dialogTitle = "${getString(R.string.range_size)} (1 - ${Constants.HUE_MEASURE_RANGE.second.toInt()})"
        range_size_edittext.setOnPreferenceChangeListener { _, new_value ->
            val new_range_size = (new_value as String).toInt()
            return@setOnPreferenceChangeListener 1 <= new_range_size && new_range_size <= Constants.HUE_MEASURE_RANGE.second.toInt()
        }
        preferenceScreen.addPreference(range_size_edittext)
    }


    private fun createEditText(key: String, title: String, summary: Preference.SummaryProvider<EditTextPreference>, input_type: Int?=null) : EditTextPreference {
        val pref = EditTextPreference(preferenceScreen.context)
        pref.key = key
        pref.title = title
        pref.dialogTitle = pref.title
        pref.summaryProvider = summary

        if (input_type != null) {
            pref.setOnBindEditTextListener { edit_text -> edit_text.inputType = input_type }
        }

        return pref
    }

}
