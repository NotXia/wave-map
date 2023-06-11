package com.example.wavemap.ui.settings.edittexts

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.EditTextPreference

class EditFloatPreference : EditTextPreference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        return getPersistedFloat(defaultReturnValue?.toFloat() ?: 0f).toString()
    }

    override fun persistString(value: String): Boolean {
        return persistFloat(value.toFloat())
    }
}

