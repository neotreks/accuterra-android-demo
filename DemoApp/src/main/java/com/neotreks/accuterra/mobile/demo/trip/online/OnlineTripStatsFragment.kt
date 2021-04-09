package com.neotreks.accuterra.mobile.demo.trip.online

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.FragmentOnlineTripStatsBinding
import com.neotreks.accuterra.mobile.demo.settings.DistanceFormatter
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.demo.settings.SpeedFormatter

/**
 * Trip Statistics fragment
 */
class OnlineTripStatsFragment : OnlineTripFragment() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: FragmentOnlineTripStatsBinding

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnlineTripStatsBinding.inflate(inflater, container, false)
        return binding.root
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

            binding.fragmentTripStatsLength.text = distanceFormatter.formatDistance(requireContext(), trip.statistics.length)
            binding.fragmentTripStatsDrivingTime.text = timeFormatter.formatDrivingTime(requireContext(), trip.statistics.drivingTime)

            binding.fragmentTripStatsCumulativeAscent.text = getDistance(distanceFormatter, trip.statistics.cumulativeAscent)
            binding.fragmentTripStatsCumulativeDescent.text = distanceFormatter.formatDistance(requireContext(), trip.statistics.cumulativeDescent)

            binding.fragmentTripStatsMinElevation.text = getDistance(distanceFormatter, trip.statistics.minElevation)
            binding.fragmentTripStatsMaxElevation.text = getDistance(distanceFormatter, trip.statistics.maxElevation)

            binding.fragmentTripStatsMaxSpeed.text = getSpeed(speedFormatter, trip.statistics.maxSpeed)
            binding.fragmentTripStatsAvgSpeed.text = getSpeed(speedFormatter, trip.statistics.avgSpeed)

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