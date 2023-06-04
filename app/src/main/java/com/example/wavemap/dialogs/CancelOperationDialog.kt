package com.example.wavemap.dialogs

import com.example.wavemap.R

class CancelOperationDialog(
    title_id : Int,
    desc_id: Int?,
    val onCancel: () -> Unit
) : PromptDialog(
    title_id,
    desc_id,
    R.string.stop,
    R.string.cancel
) {

    companion object {
        const val TAG = "Cancel-Operation-Dialog"
    }

    override fun onPositive() {
        onCancel()
    }

    override fun onNegative() {
    }

}
