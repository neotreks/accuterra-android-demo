package com.neotreks.accuterra.mobile.demo.trip.online

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.settings.DistanceFormatter
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.demo.settings.SpeedFormatter
import kotlinx.android.synthetic.main.fragment_online_trip_stats.view.*

/**
 * Trip Statistics fragment
 */
class OnlineTripStatsFragment : OnlineTripFragment() {

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_online_trip_stats, container, false)
    }

    /** Load trip data into the UI */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load trip data into the UI
        viewModel.trip.observe(viewLifecycleOwner, Observer { trip ->
            if (trip == null) {
                return@Observer
            }

            val distanceFormatter = Formatter.getDistanceFormatter()
            val timeFormatter = Formatter.getDrivingTimeFormatter()
            val speedFormatter = Formatter.getSpeedFormatter()

            view.fragment_trip_stats_length.text = distanceFormatter.formatDistance(requireContext(), trip.statistics.length)
            view.fragment_trip_stats_driving_time.text = timeFormatter.formatDrivingTime(requireContext(), trip.statistics.drivingTime)

            view.fragment_trip_stats_cumulative_ascent.text = getDistance(distanceFormatter, trip.statistics.cumulativeAscent)
            view.fragment_trip_stats_cumulative_descent.text = distanceFormatter.formatDistance(requireContext(), trip.statistics.cumulativeDescent)

            view.fragment_trip_stats_min_elevation.text = getDistance(distanceFormatter, trip.statistics.minElevation)
            view.fragment_trip_stats_max_elevation.text = getDistance(distanceFormatter, trip.statistics.maxElevation)

            view.fragment_trip_stats_max_speed.text = getSpeed(speedFormatter, trip.statistics.maxSpeed)
            view.fragment_trip_stats_avg_speed.text = getSpeed(speedFormatter, trip.statistics.avgSpeed)

        })
    }

    private fun getDistance(formatter: DistanceFormatter, value: Double?) : String {
        if (value == null) {
            return requireContext().getString(R.string.general_na)
        }
        return formatter.formatDistance(requireContext(), value)
    }

    private fun getSpeed(formatter: SpeedFormatter, value: Float?) : String {
        if (value == null) {
            return requireContext().getString(R.string.general_na)
        }
        return formatter.formatSpeed(requireContext(), value)
    }

}