package com.example.wavemap.ui.main.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.wavemap.R

class MainViewModel(application : Application) : AndroidViewModel(application) {

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

    fun startMeasuring(sampler: SamplerHandler) {
        sampler.currently_measuring.value = true
    }

    fun endMeasuring(sampler: SamplerHandler) {
        sampler.currently_measuring.value = false
    }
}