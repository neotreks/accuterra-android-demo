package com.neotreks.accuterra.mobile.demo.settings

import android.content.Context

interface LatLonFormatter {
    fun formatLatLon(context: Context, value: Double): String
}