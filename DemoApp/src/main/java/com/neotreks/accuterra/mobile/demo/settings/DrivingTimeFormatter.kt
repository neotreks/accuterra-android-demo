package com.neotreks.accuterra.mobile.demo.settings

import android.content.Context

interface DrivingTimeFormatter {
    fun formatDrivingTime(context: Context, timeInSeconds: Long): String
}