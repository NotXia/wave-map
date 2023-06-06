package com.example.wavemap.utilities

class Event<DataType>(
    private val data: DataType
) {
    private var read = false

    fun get() : DataType? {
        return when (read) {
            true -> null
            false -> {
                read = true
                data
            }
        }
    }
}