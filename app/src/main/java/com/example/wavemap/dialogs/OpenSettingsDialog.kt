package com.example.wavemap.dialogs

import com.example.wavemap.R
import com.example.wavemap.utilities.Permissions

abstract class OpenSettingsDialog(
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

        class App(title_id: Int, message_id: Int?) : OpenSettingsDialog(title_id, message_id) {
            override fun openSettings() { Permissions.openAppSettings(requireContext()) }
        }

        class WiFi(title_id: Int, message_id: Int?) : OpenSettingsDialog(title_id, message_id) {
            override fun openSettings() { Permissions.openWiFiSettings(requireContext()) }
        }

        class Bluetooth(title_id: Int, message_id: Int?) : OpenSettingsDialog(title_id, message_id) {
            override fun openSettings() { Permissions.openBluetoothSettings(requireContext()) }
        }
    }

    abstract fun openSettings()

    override fun onPositive() {
        openSettings()
    }

    override fun onNegative() {}

}