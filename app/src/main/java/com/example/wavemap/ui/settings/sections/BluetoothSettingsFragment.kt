package com.example.wavemap.ui.settings.sections

import com.example.wavemap.R
import com.example.wavemap.utilities.Constants

class BluetoothSettingsFragment :
    MeasureSettingsFragment(
        R.string.bluetooth_settings,
        "bluetooth",
        "dBm",
        Constants.BLUETOOTH_DEFAULT_RANGE_BAD,
        Constants.BLUETOOTH_DEFAULT_RANGE_GOOD,
        1,
        Constants.RANGE_SIZE_DEFAULT
    )
