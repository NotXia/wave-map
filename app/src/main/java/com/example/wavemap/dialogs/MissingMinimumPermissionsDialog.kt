package com.example.wavemap.dialogs

import com.example.wavemap.R
import com.example.wavemap.utilities.Permissions
import kotlin.system.exitProcess

class MissingMinimumPermissionsDialog() : PromptDialog(
    R.string.missing_minimum_permissions,
    R.string.missing_minimum_permissions_desc,
    R.string.settings,
    R.string.exit
) {

    companion object {
        const val TAG = "Missing-GPS-Permissions-Dialog"
    }

    override fun onPositive() {
        Permissions.openAppSettings(requireContext())
    }

    override fun onNegative() {
        exitProcess(0)

    }

}
