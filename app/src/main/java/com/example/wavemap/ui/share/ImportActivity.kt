package com.example.wavemap.ui.share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.wavemap.R
import com.example.wavemap.dialogs.CancelOperationDialog
import com.example.wavemap.dialogs.LoadingDialog
import com.example.wavemap.ui.main.MainActivity
import com.example.wavemap.ui.share.viewmodels.ImportViewModel
import java.text.SimpleDateFormat
import java.util.*


class ImportActivity : AppCompatActivity() {

    private lateinit var title_textview : TextView
    private lateinit var date_textview : TextView
    private lateinit var size_textview : TextView
    private lateinit var import_button : Button

    private var loading_dialog : LoadingDialog = LoadingDialog()

    private val view_model : ImportViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        title_textview = findViewById(R.id.import_title_textview)
        date_textview = findViewById(R.id.import_date_textview)
        size_textview = findViewById(R.id.import_size_textview)
        import_button = findViewById(R.id.file_import_button)


        val intent_uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.extras?.getParcelable(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    intent.extras?.get(Intent.EXTRA_STREAM) as Uri
                }
            else ->  null
        }
        if (intent_uri == null) {
            invalidFormat()
            return
        }


        view_model.parse_success.observe(this) { parse_success ->
            loading_dialog.dismiss()

            if (!parse_success) {
                invalidFormat()
                return@observe
            } else {
                date_textview.text = getString(R.string.export_date, SimpleDateFormat.getDateTimeInstance().format(Date(view_model.import_data.timestamp)))
                size_textview.text = getString(R.string.import_size, view_model.import_data.measures.size)
                import_button.setOnClickListener {
                    // Handles import to db
                    loading_dialog.show(supportFragmentManager, LoadingDialog.TAG)
                    import_button.isClickable = false
                    view_model.import()
                }
            }
        }

        view_model.import_success.observe(this) { import_success ->
            loading_dialog.dismiss()

            if (import_success) {
                backToMainApp()
            } else {
                Toast.makeText(baseContext, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
                import_button.isClickable = true
            }
        }


        // Input parse
        loading_dialog.show(supportFragmentManager, LoadingDialog.TAG)
        view_model.parse(intent_uri)
    }

    override fun onDestroy() {
        super.onDestroy()
        view_model.cancel()
    }


    private fun invalidFormat() {
        title_textview.text = getText(R.string.invalid_format)
        date_textview.text = ""
        size_textview.text = ""
        import_button.visibility = View.INVISIBLE
    }

    private fun backToMainApp() {
        val i = Intent(applicationContext,  MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        finish()
    }

}