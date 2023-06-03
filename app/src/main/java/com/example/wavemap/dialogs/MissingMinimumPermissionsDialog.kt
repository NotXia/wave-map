package com.example.wavemap.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.wavemap.R
import com.example.wavemap.utilities.Permissions
import kotlin.system.exitProcess

class MissingMinimumPermissionsDialog : DialogFragment() {

    companion object {
        const val TAG = "Missing-GPS-Permissions-Dialog"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setTitle(R.string.missing_minimum_permissions)
            builder.setMessage(R.string.missing_minimum_permissions_desc)
                .setPositiveButton(R.string.settings) { _, _ ->
                    Permissions.openAppSettings(requireContext())
                    this.dismiss()
                }
                .setNegativeButton(R.string.exit) { _, _ ->
                    exitProcess(0);
                    this.dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}
