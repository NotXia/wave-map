package com.example.wavemap.ui.settings.sections

import com.example.wavemap.utilities.Constants

class NoiseSettingsFragment :
    MeasureSettingsFragment(
        "noise",
        "dB",
        Constants.NOISE_DEFAULT_RANGE_BAD,
        Constants.NOISE_DEFAULT_RANGE_GOOD,
        1,
        Constants.RANGE_SIZE_DEFAULT
    )

