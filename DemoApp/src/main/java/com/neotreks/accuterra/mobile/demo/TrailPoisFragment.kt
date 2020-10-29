package com.neotreks.accuterra.mobile.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.neotreks.accuterra.mobile.demo.trail.TrailPoiDetailActivity
import com.neotreks.accuterra.mobile.demo.trail.WaypointListAdapter
import com.neotreks.accuterra.mobile.demo.ui.OnListItemClickedListener
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailDriveWaypoint
import kotlinx.android.synthetic.main.fragment_trail_pois.view.*

class TrailPoisFragment: TrailInfoFragment() {

    private val viewModel: TrailInfoViewModel by activityViewModels()

    /** Provide view layout */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trail_pois, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.drive.observe(this.viewLifecycleOwner, Observer { drive ->
            val pois = drive.waypoints
            // Create POI adapter
            val trailPoiListAdapter = WaypointListAdapter(
                requireContext(),
                pois.toMutableList(),
                displayDescription = true,
                displayAction = false
            )
            // Attach the adapter
            view.fragment_trail_pois_list.adapter = trailPoiListAdapter
            // Display POI detail
            trailPoiListAdapter.setOnListItemClickedListener(object: OnListItemClickedListener<TrailDriveWaypoint> {
                override fun onListItemClicked(item: TrailDriveWaypoint, view: View) {
                    val intent = TrailPoiDetailActivity.createNavigateToIntent(requireContext(), item.id)
                    startActivity(intent)
                }
            })
        })

    }
}