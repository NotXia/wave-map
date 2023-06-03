package com.example.wavemap.dialogs

import com.example.wavemap.utilities.Permissions

class OpenWiFiSettingsDialog(
    title_id: Int,
    message_id: Int?
) : OpenSettingsDialog(title_id, message_id) {

    companion object {
        const val TAG = "Open-WiFi-Settings-Dialog"
    }

    override fun openSettings() {
        Permissions.openWiFiSettings(requireContext())
    }

}