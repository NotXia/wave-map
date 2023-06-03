package com.example.wavemap.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.wavemap.R
import com.example.wavemap.utilities.Misc.Companion.isBatteryOptimizationOn
import com.example.wavemap.utilities.Permissions


class BackgroundServicePermissionsDialog(
    private val onGranted: () -> Unit
) : DialogFragment() {

    companion object {
        const val TAG = "Background-Service-Permissions-Dialog"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setTitle(R.string.missing_background_gps_permission)
            builder.setMessage(R.string.missing_background_gps_permission_desc)
                .setPositiveButton(R.string.settings) { _, _ ->
                    Permissions.openAppSettings(requireContext())

                    if (isBatteryOptimizationOn(requireContext())) {
                        val dialog = BatteryOptimizationDialog{ onGranted() }
                        dialog.show(parentFragmentManager, BatteryOptimizationDialog.TAG)
                    } else {
                        onGranted()
                    }

                    this.dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}
