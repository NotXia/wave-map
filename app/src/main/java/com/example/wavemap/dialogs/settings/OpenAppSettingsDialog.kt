package com.example.wavemap.dialogs.settings

import com.example.wavemap.R
import com.example.wavemap.dialogs.PromptDialog
import com.example.wavemap.utilities.Permissions

abstract class OpenAppSettingsDialog(
    title_id: Int,
    message_id: Int?
) : PromptDialog(
    title_id,
    message_id,
    R.string.settings,
    R.string.cancel
) {

    companion object {
        const val TAG = "Open-Settings-Dialog"

        class WiFi(title_id: Int, message_id: Int?) : OpenAppSettingsDialog(title_id, message_id) {
            override fun openSettings() { Permissions.openWiFiSettings(requireContext()) }
        }

        class Bluetooth(title_id: Int, message_id: Int?) : OpenAppSettingsDialog(title_id, message_id) {
            override fun openSettings() { Permissions.openBluetoothSettings(requireContext()) }
        }
    }

    open fun openSettings() {
        Permissions.openAppSettings(requireContext())
    }

    override fun onPositive() {
        openSettings()
    }

    override fun onNegative() {}

}