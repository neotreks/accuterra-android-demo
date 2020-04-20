package com.neotreks.accuterra.mobile.demo.settings

import android.content.Context

interface DistanceFormatter {
    fun formatDistance(context: Context, distanceInMeters: Double): String
}