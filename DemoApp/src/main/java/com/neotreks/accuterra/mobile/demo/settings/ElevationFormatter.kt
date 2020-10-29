package com.neotreks.accuterra.mobile.demo.settings

import android.content.Context

interface ElevationFormatter {
    fun formatElevation(context: Context, elevationInMeters: Double): String
}