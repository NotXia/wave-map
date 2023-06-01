package com.example.wavemap.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.wavemap.R
import com.example.wavemap.utilities.Permissions

class OpenSettingsDialog(
    private val title_id: Int,
    private val message_id: Int
) : DialogFragment() {

    companion object {
        const val TAG = "Missing-Permissions-Dialog"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setTitle(title_id)
            builder.setMessage(message_id)
                .setPositiveButton(R.string.settings) { _, _ ->
                    Permissions.openSettings(requireContext())
                    this.dismiss()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    this.dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}
