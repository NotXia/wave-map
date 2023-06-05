package com.example.wavemap.ui.share.viewmodels

import android.app.Application
import android.os.Environment
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
import java.io.*

class FileExportViewModel(private val application : Application) : AndroidViewModel(application) {

    val export_success = MutableLiveData<Boolean>()
    val download_success = MutableLiveData<Boolean>()
    lateinit var export_path : File

    private val db = Room.databaseBuilder(application.applicationContext, WaveDatabase::class.java, Constants.DATABASE_NAME).build()
    private var export_to_cache_job : Job? = null
    private var download_job : Job? = null

    fun createExportInCache() {
        export_to_cache_job = viewModelScope.launch(Dispatchers.IO) {
            val file_name = "${System.currentTimeMillis()}.wavemap"
            val out_dir = File(application.applicationContext.cacheDir, "/shared")
            export_path = File(out_dir, file_name)

            try {
                val exported_data = ShareMeasures.export(db)
                out_dir.mkdir()
                val fout = FileOutputStream(export_path)

                fout.write(exported_data.toByteArray())
                fout.close()
                export_success.postValue(true)
            }
            catch (err: Exception) {
                export_success.postValue(false)
            }
        }
    }

    fun downloadExport() {
        download_job = viewModelScope.launch(Dispatchers.IO) {
            try {
                val destination_path = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), export_path.name)
                val fin: InputStream = FileInputStream(export_path)
                val fout: OutputStream = FileOutputStream(destination_path)
                val buf = ByteArray(1024)
                var len: Int

                while (fin.read(buf).also { len = it } > 0) {
                    fout.write(buf, 0, len)
                }

                fin.close()
                fout.close()
                download_success.postValue(true)
            } catch (err: Exception) {
                download_success.postValue(false)
            }
        }
    }

    fun cancel() {
        export_to_cache_job?.cancel()
        download_job?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        export_path.delete()
    }

}