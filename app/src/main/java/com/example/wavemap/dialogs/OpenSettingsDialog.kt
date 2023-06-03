package com.example.wavemap.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.wavemap.R
import com.example.wavemap.utilities.Permissions

abstract class OpenSettingsDialog(
    private val title_id: Int,
    private val message_id: Int?
) : DialogFragment() {

    abstract fun openSettings()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setTitle(title_id)
            if (message_id != null) { builder.setMessage(message_id) }
            builder.setPositiveButton(R.string.settings) { _, _ ->
                openSettings()
                this.dismiss()
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                this.dismiss()
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}
