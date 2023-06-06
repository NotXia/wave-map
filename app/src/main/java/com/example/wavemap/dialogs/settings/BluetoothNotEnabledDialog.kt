package com.example.wavemap.dialogs.settings

import com.example.wavemap.R
import com.example.wavemap.utilities.Permissions

class BluetoothNotEnabledDialog : OpenAppSettingsDialog(
    R.string.bluetooth_not_enabled,
    null
) {

    companion object {
        const val TAG = "BT-Not-Enabled-Dialog"
    }

    override fun openSettings() {
        Permissions.openBluetoothSettings(requireContext())
    }

}
