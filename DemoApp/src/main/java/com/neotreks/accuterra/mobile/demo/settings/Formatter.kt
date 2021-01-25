package com.neotreks.accuterra.mobile.demo.settings

import android.content.Context
import android.text.format.DateUtils
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.util.roundTo

object Formatter {
    fun getDistanceFormatter(): DistanceFormatter {
        return object: DistanceFormatter {
            override fun formatDistance(context: Context, distanceInMeters: Float): String {
                val distanceInMiles = distanceInMeters * 0.000621371

                return if (distanceInMiles >= 1.0) {
                    context.getString(
                        R.string.distance_with_unit,
                        distanceInMiles,
                        context.getString(R.string.mile_unit_abbr))
                } else {
                    val distanceInFeet = distanceInMeters * 3.28084

                    context.getString(
                        R.string.distance_with_unit,
                        distanceInFeet,
                        context.getString(R.string.foot_unit_abbr))
                }
            }

        }
    }
    fun getSpeedFormatter(): SpeedFormatter {
        return object: SpeedFormatter {
            override fun formatSpeed(context: Context, speedInMetersPerSecond: Float): String {
                return context.getString(
                    R.string.speed_with_unit,
                    speedInMetersPerSecond * 2.23693629,
                    context.getString(R.string.mph_unit_abbr))
            }
        }
    }
    fun getHeadingFormatter(): HeadingFormatter {
        return object: HeadingFormatter {
            override fun formatHeading(context: Context, heading: Float): String {
                return context.getString(
                    R.string.heading_with_unit,
                    heading,
                    context.getString(R.string.heading_unit_abbr))
            }
        }
    }
    fun getElevationFormatter(): ElevationFormatter {
        return object: ElevationFormatter {
            override fun formatElevation(context: Context, elevationInMeters: Double): String {
                return context.getString(
                    R.string.elevation_with_unit,
                    elevationInMeters * 3.28084,
                    context.getString(R.string.foot_unit_abbr))
            }
        }
    }
    fun getLatLonFormatter(): LatLonFormatter {
        return object: LatLonFormatter {
            override fun formatLatLon(context: Context, value: Double): String {
                return value.roundTo(5).toString()
            }
        }
    }

    fun getDrivingTimeFormatter(): DrivingTimeFormatter {
        return object: DrivingTimeFormatter {
            override fun formatDrivingTime(context: Context, timeInSeconds: Int): String {
                return DateUtils.formatElapsedTime(timeInSeconds.toLong())
            }
        }
    }
}