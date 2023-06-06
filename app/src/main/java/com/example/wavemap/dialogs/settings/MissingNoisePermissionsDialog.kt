package com.example.wavemap.dialogs.settings

import com.example.wavemap.R

class MissingNoisePermissionsDialog : OpenAppSettingsDialog(
    R.string.missing_noise_permission,
    R.string.missing_noise_permission_desc
) {

    companion object {
        const val TAG = "Missing-Noise-Permissions-Dialog"
    }

}
