package com.example.wavemap.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.wavemap.R
import com.example.wavemap.utilities.Permissions

class BatteryOptimizationDialog(val onGranted: () -> Unit): DialogFragment() {

    companion object {
        const val TAG = "Battery-Optimization-Dialog"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setTitle(R.string.battery_optimization_permission)
            builder.setMessage(R.string.battery_optimization_permission_desc)
                .setPositiveButton(R.string.settings) { _, _ ->
                    Permissions.openAppSettings(requireContext())
                    onGranted()
                    this.dismiss()
                }
                .setNegativeButton(R.string.cancel) { _, _ -> this.dismiss() }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}