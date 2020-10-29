package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.neotreks.accuterra.mobile.demo.R
import kotlinx.android.synthetic.main.fragment_recorded_trip_info.view.*

/**
 * Recorded trip Map fragment
 */
class RecordedTripInfoFragment : RecordedTripFragment() {

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recorded_trip_info, container, false)
    }

    /** Load trip data into the UI */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load trip data into the UI
        viewModel.trip.observe(viewLifecycleOwner, Observer { trip ->
            if (trip == null) return@Observer
            view.fragment_recorded_trip_info_share.text = trip.userInfo.sharingType.getName()
            trip.userInfo.userRating?.let { rating ->
                view.fragment_recorded_trip_info_my_rating.rating = rating.toFloat()
                }
            view.fragment_recorded_trip_info_promote.isChecked = trip.userInfo.promoteToTrail
            view.fragment_recorded_trip_info_trip_personal_note.text = trip.userInfo.personalNote
        })
    }

}