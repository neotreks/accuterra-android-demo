package com.neotreks.accuterra.mobile.demo.trip.online

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.neotreks.accuterra.mobile.demo.databinding.FragmentOnlineTripInfoBinding

/**
 * Trip Info fragment
 */
class OnlineTripInfoFragment : OnlineTripFragment() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: FragmentOnlineTripInfoBinding

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnlineTripInfoBinding.inflate(inflater, container, false)
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
            binding.fragmentTripInfoShare.text = trip.userInfo.sharingType.getName()
            trip.userInfo.userRating?.let { rating ->
                binding.fragmentTripInfoMyRating.rating = rating
            }
            binding.fragmentTripInfoPromote.isChecked = trip.userInfo.promoteToTrail
        })
    }

}