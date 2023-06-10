package com.example.wavemap.dialogs.settings

import com.example.wavemap.R

class MissingDiskPermissionsDialog : OpenAppSettingsDialog(
    R.string.missing_disk_permission,
    R.string.missing_disk_permission_desc
) {

    companion object {
        const val TAG = "Missing-Disk-Permissions-Dialog"
    }

}
