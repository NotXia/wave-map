package com.example.wavemap.ui.settings.sections

import com.example.wavemap.utilities.Constants

class LTESettingsFragment :
    MeasureSettingsFragment(
        "lte",
        "dBm",
        Constants.LTE_DEFAULT_RANGE_BAD,
        Constants.LTE_DEFAULT_RANGE_GOOD,
        1,
        Constants.RANGE_SIZE_DEFAULT
    )

