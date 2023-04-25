package com.example.wavemap

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.wavemap.measures.WaveSampler
import com.google.android.gms.maps.model.LatLng

class MeasureViewModel(application : Application) : AndroidViewModel(application) {
    var sampler : WaveSampler? = null

    var values_scale : Pair<Double, Double>? = null
    var limit : Int? = null
    val last_measure : MutableLiveData<Long> by lazy { MutableLiveData<Long>() }


    suspend fun measure() {
        sampler?.sampleAndStore()
        last_measure.postValue(System.currentTimeMillis())
    }

    suspend fun averageOf(top_left_corner: LatLng, bottom_right_corner: LatLng) : Double? {
        return sampler?.average(top_left_corner, bottom_right_corner, limit)
    }

    fun test() : String {
        return "Ciao"
    }
}


//class MeasureViewModelFactory(
//    private val sampler: WaveSampler,
//    private val limit : Int?
//) : ViewModelProvider.Factory {
//
//    override fun <T : ViewModel> create(modelClass: Class<T>) : T {
//        return MeasureViewModel(sampler, limit) as T
//    }
//
//}
