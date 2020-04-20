package com.neotreks.accuterra.mobile.demo.settings

import android.content.Context
import com.neotreks.accuterra.mobile.demo.R

object Formatter {
    fun getDistanceFormatter(): DistanceFormatter {
        return object: DistanceFormatter {
            override fun formatDistance(context: Context, distanceInMeters: Double): String {
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
}