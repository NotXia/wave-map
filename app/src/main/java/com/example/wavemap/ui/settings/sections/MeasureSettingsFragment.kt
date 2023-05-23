package com.example.wavemap.ui.settings.sections

import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.example.wavemap.R


open class MeasureSettingsFragment(
    private val key: String,
    private val measure_unit: String,
    private val range_bad_default: Double,
    private val range_good_default: Double,
    private val past_limit_default: Int
) : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.measure_preferences, rootKey)

        // Setting default values (if needed)
        val pref_manager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val editor: SharedPreferences.Editor = pref_manager.edit()

        if (pref_manager.getString("${key}_range_bad", "") == "") { editor.putString("${key}_range_bad", "$range_bad_default") }
        if (pref_manager.getString("${key}_range_good", "") == "") { editor.putString("${key}_range_good", "$range_good_default") }
        if (pref_manager.getString("${key}_past_limit", "") == "") { editor.putString("${key}_past_limit", "$past_limit_default") }
        editor.commit()

        // Creating settings elements
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
        preferenceScreen.addPreference(createEditText(
            "${key}_past_limit", getString(R.string.range_bad),
            Preference.SummaryProvider<EditTextPreference> { preference -> "${ preference.text ?: past_limit_default }" },
            InputType.TYPE_CLASS_NUMBER
        ))
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
