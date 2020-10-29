package com.neotreks.accuterra.mobile.demo.settings

import android.content.Context

interface SpeedFormatter {
    fun formatSpeed(context: Context, speedInMetersPerSecond: Float): String
}