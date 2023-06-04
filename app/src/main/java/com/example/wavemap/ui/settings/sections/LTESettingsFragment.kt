package com.example.wavemap.ui.settings.sections

import com.example.wavemap.R
import com.example.wavemap.utilities.Constants

class LTESettingsFragment :
    MeasureSettingsFragment(
        R.string.lte_settings,
        "lte",
        "dBm",
        Constants.LTE_DEFAULT_RANGE_BAD,
        Constants.LTE_DEFAULT_RANGE_GOOD,
        1,
        Constants.RANGE_SIZE_DEFAULT
    )

