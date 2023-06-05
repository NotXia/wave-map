package com.example.wavemap.ui.share.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.wavemap.db.ShareMeasures
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.utilities.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class ImportViewModel(private val application : Application) : AndroidViewModel(application) {

    val parse_success = MutableLiveData<Boolean>()
    val import_success = MutableLiveData<Boolean>()
    lateinit var import_data : ShareMeasures.ExportFormat

    private val db = Room.databaseBuilder(application.applicationContext, WaveDatabase::class.java, Constants.DATABASE_NAME).build()
    private var parse_job : Job? = null
    private var import_job : Job? = null


    fun parse(uri: Uri) {
        if (this::import_data.isInitialized) {
            parse_success.postValue(true)
            return
        }

        parse_job = viewModelScope.launch {
            try {
                import_data = ShareMeasures.parse(readFromURI(uri))
                parse_success.postValue(true)
            }
            catch (err: Exception) {
                // Invalid format
                parse_success.postValue(false)
            }
        }
    }

    fun import() {
        import_job = viewModelScope.launch(Dispatchers.IO) {
            try {
                ShareMeasures.import(db, import_data)
                import_success.postValue(true)
            }
            catch (err: Exception) {
                import_success.postValue(false)
            }
        }
    }

    fun cancel() {
        parse_job?.cancel()
        import_job?.cancel()
    }


    private fun readFromURI(uri: Uri) : String {
        var import_str = ""

        val reader = BufferedReader(InputStreamReader(application.contentResolver.openInputStream(uri)))
        var line: String?
        while (reader.readLine().also{ line = it } != null) {
            import_str += line
        }

        return import_str
    }
}