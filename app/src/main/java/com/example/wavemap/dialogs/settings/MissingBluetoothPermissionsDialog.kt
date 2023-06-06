package com.example.wavemap.dialogs.settings

import com.example.wavemap.R

class MissingBluetoothPermissionsDialog : OpenAppSettingsDialog(
    R.string.missing_bluetooth_permission,
    R.string.missing_bluetooth_permission_desc
) {

    companion object {
        const val TAG = "Missing-Bluetooth-Permissions-Dialog"
    }

}
