package com.example.wavemap.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.wavemap.R


class ShareExportDialog(
    private val onSave: () -> Unit,
    private val onShare: () -> Unit
) : DialogFragment() {

    companion object {
        const val TAG = "Share-Export-Dialog"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val view: View = requireActivity().layoutInflater.inflate(R.layout.dialog_share_export, null)
            view.findViewById<Button>(R.id.share_button).setOnClickListener {
                onShare()
            }
            view.findViewById<Button>(R.id.save_button).setOnClickListener {
                onSave()
            }

            val builder = AlertDialog.Builder(it)
            builder.setView(view)

            val dialog = builder.create()
            dialog.setCanceledOnTouchOutside(false)

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
