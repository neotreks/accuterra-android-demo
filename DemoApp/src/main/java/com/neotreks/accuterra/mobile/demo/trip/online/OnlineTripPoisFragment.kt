package com.neotreks.accuterra.mobile.demo.trip.online

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.neotreks.accuterra.mobile.demo.databinding.FragmentOnlineTripPoisBinding
import com.neotreks.accuterra.mobile.demo.ui.OnListItemClickedListener
import com.neotreks.accuterra.mobile.sdk.trip.model.TripPoint

/**
 * Trip POIs fragment
 */
class OnlineTripPoisFragment : OnlineTripFragment() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: FragmentOnlineTripPoisBinding

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOnlineTripPoisBinding.inflate(inflater, container, false)
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
            // Create POI adapter
            val tripPoiListAdapter = OnlineTripPoiListAdapter(
                requireContext(),
                trip.navigation.points
            )
            // Attach the adapter
            binding.fragmentTripPoisList.adapter = tripPoiListAdapter
            // Display POI detail
            tripPoiListAdapter.setOnListItemClickedListener(object : OnListItemClickedListener<TripPoint> {
                override fun onListItemClicked(item: TripPoint, view: View) {
                    val intent = OnlineTripPoiActivity.createNavigateToIntent(requireContext(), trip.info.uuid, item.uuid)
                    startActivity(intent)
                }
            })
        })
    }

}