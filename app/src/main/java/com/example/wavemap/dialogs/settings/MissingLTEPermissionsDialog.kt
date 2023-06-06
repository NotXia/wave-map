package com.example.wavemap.dialogs.settings

import com.example.wavemap.R

class MissingLTEPermissionsDialog : OpenAppSettingsDialog(
    R.string.missing_lte_permission,
    R.string.missing_lte_permission_desc
) {

    companion object {
        const val TAG = "Missing-LTE-Permissions-Dialog"
    }

}
