package com.example.wavemap.dialogs

import com.example.wavemap.utilities.Permissions

class OpenAppSettingsDialog(
    title_id: Int,
    message_id: Int?
) : OpenSettingsDialog(title_id, message_id) {

    companion object {
        const val TAG = "Open-App-Settings-Dialog"
    }

    override fun openSettings() {
        Permissions.openAppSettings(requireContext())
    }

}
