package com.example.wavemap.dialogs.settings

import com.example.wavemap.R

class MissingWiFiPermissionsDialog : OpenAppSettingsDialog(
    R.string.missing_wifi_permission,
    R.string.missing_wifi_permission_desc
) {

    companion object {
        const val TAG = "Missing-WiFi-Permissions-Dialog"
    }

}
