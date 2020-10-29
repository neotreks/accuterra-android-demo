package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.ui.OnListItemClickedListener
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi
import kotlinx.android.synthetic.main.fragment_recorded_trip_pois.view.*

/**
 * Recorded Trip POIs fragment
 */
class RecordedTripPoisFragment : RecordedTripFragment() {

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recorded_trip_pois, container, false)
    }

    /** Load trip data into the UI */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load trip data into the UI
        viewModel.pois.observe(viewLifecycleOwner, Observer { pois ->
            requireNotNull(pois) { IllegalStateException("Trip POIs not loaded yet!") }
            // Create POI adapter
            val tripPoiListAdapter = RecordedTripPoiListAdapter(
                requireContext(),
                pois
            )
            // Attach the adapter
            view.fragment_recorded_trip_pois_list.adapter = tripPoiListAdapter
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