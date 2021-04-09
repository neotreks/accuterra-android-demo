package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.FragmentRecordedTripStatsBinding
import com.neotreks.accuterra.mobile.demo.settings.DistanceFormatter
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.demo.settings.SpeedFormatter

/**
 * Recorded trip Statistics fragment
 */
class RecordedTripStatsFragment : RecordedTripFragment() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: FragmentRecordedTripStatsBinding

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecordedTripStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Load trip data into the UI */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load trip data into the UI
        viewModel.trip.observe(viewLifecycleOwner, Observer { trip ->
            if (trip == null) return@Observer

            val distanceFormatter = Formatter.getDistanceFormatter()
            val timeFormatter = Formatter.getDrivingTimeFormatter()
            val speedFormatter = Formatter.getSpeedFormatter()

            binding.fragmentRecordedTripStatsLength.text = distanceFormatter.formatDistance(requireContext(), trip.tripStatistics.length)
            binding.fragmentRecordedTripStatsDrivingTime.text = timeFormatter.formatDrivingTime(requireContext(), trip.tripStatistics.drivingTime)

            binding.fragmentRecordedTripStatsCumulativeAscent.text = getDistance(distanceFormatter, trip.tripStatistics.cumulativeAscent)
            binding.fragmentRecordedTripStatsCumulativeDescent.text = distanceFormatter.formatDistance(requireContext(), trip.tripStatistics.cumulativeDescent)

            binding.fragmentRecordedTripStatsMinElevation.text = getDistance(distanceFormatter, trip.tripStatistics.minElevation)
            binding.fragmentRecordedTripStatsMaxElevation.text = getDistance(distanceFormatter, trip.tripStatistics.maxElevation)

            binding.fragmentRecordedTripStatsMaxSpeed.text = getSpeed(speedFormatter, trip.tripStatistics.maxSpeed)
            binding.fragmentRecordedTripStatsAvgSpeed.text = getSpeed(speedFormatter, trip.tripStatistics.avgSpeed)

        })
    }

    private fun getDistance(formatter: DistanceFormatter, value: Float?) : String {
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