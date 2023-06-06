package com.example.wavemap.dialogs.settings

import com.example.wavemap.R
import com.example.wavemap.utilities.Permissions

class WiFiNotEnabledDialog : OpenAppSettingsDialog(
    R.string.wifi_not_enabled,
    null
) {

    companion object {
        const val TAG = "WiFi-Not-Enabled-Dialog"
    }

    override fun openSettings() {
        Permissions.openWiFiSettings(requireContext())
    }

}
