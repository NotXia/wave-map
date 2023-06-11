package com.example.wavemap.ui.main.viewmodels

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.example.wavemap.R
import com.example.wavemap.dialogs.MeasureFilterDialog
import com.example.wavemap.services.BackgroundScanService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(private val application : Application) : AndroidViewModel(application) {

    class SamplerHandler(
        val view_model : MeasureViewModel,
        val label : String
    ) {
        var currently_measuring : MutableLiveData<Boolean> = MutableLiveData<Boolean>(false) // To prevent multiple measure requests
    }


    val available_samplers = arrayOf(
        SamplerHandler( WiFiViewModel(application), application.resources.getString(R.string.wifi) ),
        SamplerHandler( LTEViewModel(application), application.resources.getString(R.string.lte) ),
        SamplerHandler( NoiseViewModel(application), application.resources.getString(R.string.noise) ),
        SamplerHandler( BluetoothViewModel(application), application.resources.getString(R.string.bluetooth) )
    )
    var curr_sampler_index = 0
    val curr_sampler : SamplerHandler
        get() = available_samplers[curr_sampler_index]

    private val pref_manager = PreferenceManager.getDefaultSharedPreferences(application.applicationContext)

    private var periodic_scan_handler: Handler? = null

    var should_refresh_map = MutableLiveData<Boolean>(false)
    var user_measure_error = MutableLiveData<Exception?>(null)
    var auto_measure_error = MutableLiveData<Exception?>(null)


    fun changeSampler(index: Int) {
        curr_sampler_index = index
        curr_sampler.view_model.loadSettingsPreferences()
    }


    fun getSamplerQueries(sampler: SamplerHandler) : List<Pair<String, String?>> {
        if (sampler.view_model !is QueryableMeasureViewModel) { return arrayListOf() }
        val queryable_model : QueryableMeasureViewModel = sampler.view_model

        return listOf(Pair(application.getString(R.string.remove_filter), null)) + queryable_model.listQueries()
    }


    private suspend fun measure(sampler: SamplerHandler) {
        if (sampler.currently_measuring.value == true) { return }

        sampler.currently_measuring.postValue(true)
        sampler.view_model.measure()
        if (sampler.view_model == curr_sampler.view_model) {
            should_refresh_map.postValue(true)
        }
        sampler.currently_measuring.postValue(false)

    }

    fun userTriggeredMeasure(sampler: SamplerHandler) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                measure(sampler)
            }
            catch (err: Exception) {
                user_measure_error.postValue(err)
            }
        }
    }

    fun autoTriggeredMeasure() {
        available_samplers.forEach {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    measure(it)
                }
                catch (err: Exception) {
                    auto_measure_error.postValue(err)
                }
            }
        }
    }

    fun tileChangeMeasure() {
        if (pref_manager.getBoolean("tile_change_scan", false)) {
            autoTriggeredMeasure()
            resetPeriodicScan()
        }
    }


    /**
     * Foreground periodic measures
     * */

    fun startPeriodicScan() {
        if (pref_manager.getBoolean("periodic_scan", false)) {
            val delay = pref_manager.getInt("periodic_scan_interval", 60).toLong()
            if (periodic_scan_handler != null) { return }

            periodic_scan_handler = Handler(Looper.getMainLooper())
            periodic_scan_handler!!.postDelayed({
                autoTriggeredMeasure()
                resetPeriodicScan()
            }, delay * 1000)
        } else {
            stopPeriodicScan()
        }
    }

    fun stopPeriodicScan() {
        periodic_scan_handler?.removeCallbacksAndMessages(null)
        periodic_scan_handler = null
    }

    fun resetPeriodicScan() {
        stopPeriodicScan()
        startPeriodicScan()
    }

}