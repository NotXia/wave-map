package com.example.wavemap.ui.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.wavemap.R
import com.example.wavemap.services.BackgroundScanService

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container_settings, MainSettingsFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        BackgroundScanService.forceStop(this)
    }

    override fun onPause() {
        super.onPause()
        BackgroundScanService.start(this)
    }

}