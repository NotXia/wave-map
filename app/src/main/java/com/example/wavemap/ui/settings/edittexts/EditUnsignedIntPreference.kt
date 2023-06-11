package com.example.wavemap.ui.settings.edittexts

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import androidx.preference.EditTextPreference

class EditUnsignedIntPreference : EditTextPreference {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        setOnBindEditTextListener {
            it.inputType = InputType.TYPE_CLASS_NUMBER
        }
    }

    override fun getPersistedString(defaultReturnValue: String?): String {
        return getPersistedInt(defaultReturnValue?.toInt() ?: 0).toString()
    }

    override fun persistString(value: String): Boolean {
        return persistInt(value.toInt())
    }
}

