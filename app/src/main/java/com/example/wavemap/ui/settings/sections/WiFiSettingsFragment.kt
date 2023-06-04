package com.example.wavemap.ui.settings.sections

import com.example.wavemap.R
import com.example.wavemap.utilities.Constants

class WiFiSettingsFragment :
    MeasureSettingsFragment(
        R.string.wifi_settings,
        "wifi",
        "dBm",
        Constants.WIFI_DEFAULT_RANGE_BAD,
        Constants.WIFI_DEFAULT_RANGE_GOOD,
        1,
        Constants.RANGE_SIZE_DEFAULT
    )
