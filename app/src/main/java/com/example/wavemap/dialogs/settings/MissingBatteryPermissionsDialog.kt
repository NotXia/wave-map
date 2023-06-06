package com.example.wavemap.dialogs.settings

import com.example.wavemap.R

class MissingBatteryPermissionsDialog : OpenAppSettingsDialog(
    R.string.battery_optimization_permission,
    R.string.battery_optimization_permission_desc
) {

    companion object {
        const val TAG = "Missing-Battery-Permissions-Dialog"
    }

}
