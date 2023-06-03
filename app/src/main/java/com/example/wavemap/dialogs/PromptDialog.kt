/**
 * Base class for a dialog with some text and two buttons
 * */

package com.example.wavemap.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment


abstract class PromptDialog(
    private val title_id : Int,
    private val message_id : Int?,
    private val positive_button_id : Int,
    private val negative_button_id : Int,
) : DialogFragment() {

    protected abstract fun onPositive()
    protected abstract fun onNegative()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setTitle(title_id)
            if (message_id != null) { builder.setMessage(message_id) }
            builder.setPositiveButton(positive_button_id) { _, _ ->
                onPositive()
                this.dismiss()
            }
            .setNegativeButton(negative_button_id) { _, _ ->
                onNegative()
                this.dismiss()
            }
            val dialog = builder.create()
            dialog.setCanceledOnTouchOutside(false)

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}
