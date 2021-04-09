package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.neotreks.accuterra.mobile.demo.databinding.FragmentRecordedTripPoisBinding
import com.neotreks.accuterra.mobile.demo.ui.OnListItemClickedListener
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi

/**
 * Recorded Trip POIs fragment
 */
class RecordedTripPoisFragment : RecordedTripFragment() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: FragmentRecordedTripPoisBinding

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRecordedTripPoisBinding.inflate(inflater, container, false)
        return binding.root
    }

    /** Load trip data into the UI */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load trip data into the UI
        viewModel.pois.observe(viewLifecycleOwner, { pois ->
            requireNotNull(pois) { IllegalStateException("Trip POIs not loaded yet!") }
            // Create POI adapter
            val tripPoiListAdapter = RecordedTripPoiListAdapter(
                requireContext(),
                pois
            )
            // Attach the adapter
            binding.fragmentRecordedTripPoisList.adapter = tripPoiListAdapter
            // Display POI detail
            tripPoiListAdapter.setOnListItemClickedListener(object: OnListItemClickedListener<TripRecordingPoi> {
                override fun onListItemClicked(item: TripRecordingPoi, view: View) {
                    val intent = RecordedTripPoiActivity.createNavigateToIntent(item.uuid, requireContext())
                    startActivity(intent)
                }
            })
        })
    }

}