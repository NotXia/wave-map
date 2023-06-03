package com.example.wavemap.ui.main.viewmodels

import android.app.Application

abstract class QueryableMeasureViewModel(application : Application) : MeasureViewModel(application) {
    abstract fun changeQuery(new_query : String?)

    /* Returns a list of pairs of (label, query) */
    abstract fun listQueries() : List<Pair<String, String>>
}
