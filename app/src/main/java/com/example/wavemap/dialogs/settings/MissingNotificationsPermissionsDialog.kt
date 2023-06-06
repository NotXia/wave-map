package com.example.wavemap.dialogs.settings

import com.example.wavemap.R

class MissingNotificationsPermissionsDialog : OpenAppSettingsDialog(
    R.string.missing_notification_permission,
    R.string.missing_notification_permission_desc
) {

    companion object {
        const val TAG = "Missing-Notifications-Permissions-Dialog"
    }

}
