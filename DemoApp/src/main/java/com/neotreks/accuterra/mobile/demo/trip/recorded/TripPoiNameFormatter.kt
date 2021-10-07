package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi

/**
 * Utility class to format trip POI name
 */
class TripPoiNameFormatter(private val context: Context) {

    private val distanceFormatter = Formatter.getDistanceFormatter()

    /**
     * Return POI name in format like "Poi name [4.4 MI]"
     */
    fun getPoiNameWithDistanceMarker(poi: TripRecordingPoi): String {
        return poi.name + if (poi.distanceMarker == null) {
            ""
        } else {
            " [${distanceFormatter.formatDistance(context, poi.distanceMarker!!)}]"
        }
    }

    /**
     * Returns the formatter distance marker value or empty string if distance marker is not available
     */
    fun getDistanceMarker(poi: TripRecordingPoi): String {
        return if (poi.distanceMarker == null) {
            ""
        } else {
            distanceFormatter.formatDistance(context, poi.distanceMarker!!)
        }
    }

}