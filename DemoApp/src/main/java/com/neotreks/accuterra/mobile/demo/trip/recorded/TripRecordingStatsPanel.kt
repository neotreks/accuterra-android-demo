package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.location.Location
import android.widget.TextView
import com.neotreks.accuterra.mobile.demo.util.*

/**
 * Utility class to manage trip recording stats UI
 */
class TripRecordingStatsPanel(
    private val speedView: TextView,
    private val headingView: TextView,
    private val elevationView: TextView,
    private val latView: TextView,
    private val lonView: TextView,
    private val context: Context
) {

    fun updateRecordingUI(location: Location?) {

        speedView.text = location?.getSpeedInfo(context)
        headingView.text = location?.getHeadingInfo(context)

        elevationView.text = location?.getAltInfo(context)
        latView.text = location?.getLatInfo(context)
        lonView.text = location?.getLonInfo(context)

    }

}