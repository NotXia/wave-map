package com.example.wavemap.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.example.wavemap.R


class LoadingDialog() : DialogFragment() {

    companion object {
        const val TAG = "Loading-Dialog"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val view: View = requireActivity().layoutInflater.inflate(R.layout.dialog_loading, null)
            (view.findViewById<ImageView>(R.id.loading_imageview).drawable as AnimationDrawable).start()

            val builder = AlertDialog.Builder(it)
            builder.setView(view)

            val dialog = builder.create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.setCancelable(false)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setOnKeyListener { _, key_code, _ -> // Prevents dialog to close on back button
                true
            }

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
