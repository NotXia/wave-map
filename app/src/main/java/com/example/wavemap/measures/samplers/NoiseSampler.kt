package com.example.wavemap.measures.samplers

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.example.wavemap.db.MeasureTable
import com.example.wavemap.db.MeasureType
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.measures.WaveMeasure
import com.example.wavemap.measures.WaveSampler
import com.example.wavemap.utilities.LocationUtils
import com.example.wavemap.utilities.Permissions
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.log10


class NoiseSampler(
    private val context: Context,
    private val db: WaveDatabase,
    private var sample_time: Int = 5
) : WaveSampler() {

    private fun initMediaRecorder() : MediaRecorder {
        val media_recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()

        media_recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        media_recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        media_recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            media_recorder.setOutputFile( File(context.filesDir, "recording.mp3") )
        } else {
            media_recorder.setOutputFile("/dev/null")
        }
        media_recorder.prepare()

        return media_recorder
    }

    // Measures by averaging a couple of sound samples
    override suspend fun sample() : List<WaveMeasure> = suspendCoroutine { cont ->
        if ( !Permissions.check(context, Permissions.noise) ) { return@suspendCoroutine cont.resumeWithException( SecurityException("Missing noise permissions") ) }

        GlobalScope.launch {
            val media_recorder = initMediaRecorder()
            var sum_db = 0.0

            media_recorder.start()
            media_recorder.maxAmplitude  // The first call to maxAmplitude always returns 0
            for (i in 1..sample_time) {
                delay(500)

                val amplitude = media_recorder.maxAmplitude
                if (amplitude != 0) {
                    // Source: https://stackoverflow.com/questions/10655703/what-does-androids-getmaxamplitude-function-for-the-mediarecorder-actually-gi
                    val pressure = amplitude / 51805.5336
                    val db : Double = 20 * log10 (pressure/0.0002)
                    sum_db += db
                }
            }
            media_recorder.stop()
            media_recorder.release()

            sum_db /= sample_time
            val current_location: LatLng = LocationUtils.getCurrent(context)

            cont.resume(listOf( MeasureTable(MeasureType.NOISE, sum_db, System.currentTimeMillis(), current_location.latitude, current_location.longitude, "") ))
        }
    }

    override suspend fun store(measures: List<WaveMeasure>) {
        for (measure in measures) {
            db.measureDAO().insert( MeasureTable(measure.author, MeasureType.NOISE, measure.value, measure.timestamp, measure.latitude, measure.longitude, measure.info) )
        }
    }

    override suspend fun retrieve(top_left_corner: LatLng, bottom_right_corner: LatLng, limit: Int?) : List<WaveMeasure> {
        return db.measureDAO().get(
            MeasureType.NOISE,
            top_left_corner.latitude,
            top_left_corner.longitude,
            bottom_right_corner.latitude,
            bottom_right_corner.longitude,
            limit ?: -1
        )
    }
}