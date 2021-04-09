package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.neotreks.accuterra.mobile.demo.databinding.FragmentRecordedTripInfoBinding

/**
 * Recorded trip Map fragment
 */
class RecordedTripInfoFragment : RecordedTripFragment() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: FragmentRecordedTripInfoBinding

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecordedTripInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Load trip data into the UI */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load trip data into the UI
        viewModel.trip.observe(viewLifecycleOwner, Observer { trip ->
            if (trip == null) return@Observer
            binding.fragmentRecordedTripInfoShare.text = trip.userInfo.sharingType.getName()
            trip.userInfo.userRating?.let { rating ->
                binding.fragmentRecordedTripInfoMyRating.rating = rating
            }
            binding.fragmentRecordedTripInfoPromote.isChecked = trip.userInfo.promoteToTrail
            binding.fragmentRecordedTripInfoTripPersonalNote.text = trip.userInfo.personalNote
        })
    }

}