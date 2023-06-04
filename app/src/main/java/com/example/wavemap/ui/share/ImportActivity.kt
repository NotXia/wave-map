package com.example.wavemap.ui.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.wavemap.R
import com.example.wavemap.db.ShareMeasures
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.dialogs.CancelOperationDialog
import com.example.wavemap.dialogs.LoadingDialog
import com.example.wavemap.ui.main.MainActivity
import com.example.wavemap.utilities.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess


class ImportActivity : AppCompatActivity() {

    private lateinit var date_textview : TextView
    private lateinit var size_textview : TextView
    private lateinit var import_button : Button

    private lateinit var import : ShareMeasures.ExportFormat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        date_textview = findViewById(R.id.date_textview)
        size_textview = findViewById(R.id.size_textview)
        import_button = findViewById(R.id.file_import_button)

        val db = Room.databaseBuilder(applicationContext, WaveDatabase::class.java, Constants.DATABASE_NAME).build()
        val intent_uri = intent.data ?: exitProcess(0)

        try {
            import = ShareMeasures.parse(readFromURI(intent_uri))
        }
        catch (err: Exception) {
            Toast.makeText(baseContext, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
        }


        date_textview.text = getString(R.string.export_date, SimpleDateFormat.getDateTimeInstance().format(Date(import.timestamp)))
        size_textview.text = getString(R.string.import_size, import.measures.size)
        import_button.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val loading_dialog = LoadingDialog{
                        if (supportFragmentManager.findFragmentByTag(CancelOperationDialog.TAG) != null) { return@LoadingDialog }

                        CancelOperationDialog(R.string.cancel_import, null){
                            backToMainApp()
                        }.show(supportFragmentManager, CancelOperationDialog.TAG)
                    }

                    withContext(Dispatchers.Main) { loading_dialog.show(supportFragmentManager, LoadingDialog.TAG) }
                    ShareMeasures.import(db, import)
                    withContext(Dispatchers.Main) { loading_dialog.dismiss() }
                    backToMainApp()
                }
                catch (err: Exception) {
                    Toast.makeText(baseContext, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun readFromURI(uri: Uri) : String {
        var import_str = ""

        val reader = BufferedReader(InputStreamReader(contentResolver.openInputStream(uri)))
        var line: String?
        while (reader.readLine().also{ line = it } != null) {
            import_str += line
        }

        return import_str
    }

    private fun backToMainApp() {
        val i = Intent(applicationContext,  MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        finish()
    }

}