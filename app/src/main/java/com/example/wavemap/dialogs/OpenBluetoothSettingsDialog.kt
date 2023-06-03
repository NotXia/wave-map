package com.example.wavemap.dialogs

import com.example.wavemap.utilities.Permissions

class OpenBluetoothSettingsDialog(
    title_id: Int,
    message_id: Int?
) : OpenSettingsDialog(title_id, message_id) {

    companion object {
        const val TAG = "Open-Bluetooth-Settings-Dialog"
    }

    override fun openSettings() {
        Permissions.openBluetoothSettings(requireContext())
    }

}