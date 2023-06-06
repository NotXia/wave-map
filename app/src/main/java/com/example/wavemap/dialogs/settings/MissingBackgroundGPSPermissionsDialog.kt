package com.example.wavemap.dialogs.settings

import com.example.wavemap.R

class MissingBackgroundGPSPermissionsDialog : OpenAppSettingsDialog(
    R.string.missing_background_gps_permission,
    R.string.missing_background_gps_permission_desc
) {

    companion object {
        const val TAG = "Missing-BackgroundGPS-Permissions-Dialog"
    }

}
