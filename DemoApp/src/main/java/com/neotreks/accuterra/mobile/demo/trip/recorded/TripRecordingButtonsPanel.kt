package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.view.View
import android.widget.TextView
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.demo.util.visibility

/**
 * Utility class to manage trip recording buttons visibility
 */
class TripRecordingButtonsPanel(
    private val durationView: TextView,
    private val distanceView: TextView,
    private val startView: View,
    private val stopView: View,
    private val resumeView: View,
    private val finishView: View,
    private val recordPoiView: View,
    private val context: Context
) {

    private val distanceFormatter = Formatter.getDistanceFormatter()
    private val drivingTimeFormatter = Formatter.getDrivingTimeFormatter()

    fun setStartMode() {
        durationView.visibility = false.visibility
        distanceView.visibility = false.visibility
        startView.visibility = true.visibility
        stopView.visibility = false.visibility
        resumeView.visibility = false.visibility
        finishView.visibility = false.visibility
        recordPoiView.visibility = false.visibility
    }

    fun setRecordingMode() {
        durationView.visibility = true.visibility
        distanceView.visibility = true.visibility
        startView.visibility = false.visibility
        stopView.visibility = true.visibility
        resumeView.visibility = false.visibility
        finishView.visibility = false.visibility
        recordPoiView.visibility = true.visibility
    }

    fun setPausedMode() {
        durationView.visibility = false.visibility
        distanceView.visibility = false.visibility
        startView.visibility = false.visibility
        stopView.visibility = false.visibility
        resumeView.visibility = true.visibility
        finishView.visibility = true.visibility
        recordPoiView.visibility = true.visibility
    }

    fun updateStatistics(distance: Double?, drivingTime: Long?) {
        distanceView.text =
            if (distance == null) {
                context.getString(R.string.general_na)
            } else {
                distanceFormatter.formatDistance(context, distance)
            }
        durationView.text =
            if (drivingTime == null) {
                context.getString(R.string.general_na)
            } else {
                drivingTimeFormatter.formatDrivingTime(context, drivingTime)
            }
    }

}