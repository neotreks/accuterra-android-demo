package com.neotreks.accuterra.mobile.demo.util

import android.content.Context
import android.location.Location
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.settings.Formatter

/**
 * Location related extensions
 */


fun Location.getSpeedInfo(context: Context): String {
    return if (hasSpeed()) {
        Formatter.getSpeedFormatter().formatSpeed(context, speed)
    } else {
        context.getString(R.string.general_na)
    }
}

fun Location.getHeadingInfo(context: Context): String {
    return if (hasBearing()) {
        Formatter.getHeadingFormatter().formatHeading(context, bearing)
    } else {
        context.getString(R.string.general_na)
    }
}

fun Location.getAltInfo(context: Context): String {
    return if (hasAltitude()) {
        Formatter.getElevationFormatter().formatElevation(context, altitude)
    } else {
        context.getString(R.string.general_na)
    }
}

fun Location.getLatInfo(context: Context): String {
    return Formatter.getLatLonFormatter().formatLatLon(context, latitude)
}

fun Location.getLonInfo(context: Context): String {
    return Formatter.getLatLonFormatter().formatLatLon(context, longitude)
}