package com.example.wavemap.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.wavemap.R

class MeasureFilterDialog(
    private val items : Array<CharSequence>,
    private val onSelected : (index: Int) -> Unit
) : DialogFragment() {

    companion object {
        const val TAG = "Measure-Filter-Dialog"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setTitle(getString(R.string.filter_measures))
            builder.setItems(items) { _, index ->
                onSelected(index)
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}
