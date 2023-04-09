package com.example.wavemap.measures

abstract class WaveSampler {
    /**
     * @param onData    Called when the measure is available
     * @param onError   Called on error
     * */
    abstract fun sample(onData :(measure :Double)->Unit, onError :(error :Int)->Unit) :Unit

    /**
     * @param measure       Value to store
     * @param onSuccess     Called after the value has been saved
     * @param onError       Called on error
     * */
    abstract fun store(measure :Double, onSuccess :()->Unit, onError :(error :Int)->Unit) :Unit

    /**
     * @param onData    Called after the measure has been saved
     * @param onError   Called on error
     * */
    fun sampleAndStore(onData :(data :Double)->Unit, onError :(error :Int)->Unit) :Unit {
        sample(
            fun (data) {
                store(data, fun () { onData(data) }, onError)
            },
            fun (error) {
                onError(error)
            }
        )
    }
}