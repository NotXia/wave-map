package com.example.wavemap.measures

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.wavemap.db.MeasureTable
import com.example.wavemap.db.MeasureType
import com.example.wavemap.db.WaveDatabase
import com.example.wavemap.utilities.LocationUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.log10


class NoiseSampler : WaveSampler {
    private val context : Context
    private val db : WaveDatabase
    private var sample_amount : Int

    constructor(context: Context, db: WaveDatabase, sample_time: Int=10) {
        this.context = context
        this.db = db
        this.sample_amount = sample_time
    }

    private fun initMediaRecorder() : MediaRecorder {
        var media_recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else{
            MediaRecorder()
        }

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

    override suspend fun sample() : List<WaveMeasure> = suspendCoroutine { cont ->
        if ( ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ) {
            return@suspendCoroutine cont.resumeWithException( SecurityException("Missing RECORD_AUDIO permissions") )
        }

        GlobalScope.launch() {
            val media_recorder = initMediaRecorder()
            var sum_db : Double = 0.0

            media_recorder.start()
            media_recorder.maxAmplitude  // the first call to maxAmplitude always return 0
            for (i in 1..sample_amount) {
                delay(100)

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

            sum_db /= sample_amount
            val current_location: LatLng = LocationUtils.getCurrent(context)

            cont.resume(listOf( MeasureTable(0, MeasureType.NOISE, sum_db, System.currentTimeMillis(), current_location.latitude, current_location.longitude, "") ))
        }
    }

    override suspend fun store(measures: List<WaveMeasure>) : Unit {
        for (measure in measures) {
            db.measureDAO().insert( MeasureTable(0, MeasureType.NOISE, measure.value, measure.timestamp, measure.latitude, measure.longitude, measure.info) )
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