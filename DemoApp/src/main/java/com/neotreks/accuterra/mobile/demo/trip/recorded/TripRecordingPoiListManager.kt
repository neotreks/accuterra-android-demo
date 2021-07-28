package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.view.View
import android.widget.ListView
import com.neotreks.accuterra.mobile.demo.ui.OnListItemClickedListener
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi

/**
 * Utility class to manage the POIs list component
 *
 * @see [refreshAdapter]
 */
class TripRecordingPoiListManager(
    private val context: Context,
    private val view: ListView,
    private val clickListener: TripRecordingPoiListClickListener,
) {

    private lateinit var adapter: RecordedTripPoiListAdapter

    fun refreshAdapter(poiList: List<TripRecordingPoi>) {
        // Setup the adapter
        adapter = RecordedTripPoiListAdapter(context, poiList)
        adapter.setOnListItemClickedListener(object: OnListItemClickedListener<TripRecordingPoi> {
            override fun onListItemClicked(item: TripRecordingPoi, view: View) {
                clickListener.onItemClicked(item)
            }
        })
        view.adapter = adapter
    }

    interface TripRecordingPoiListClickListener {
        fun onItemClicked(item: TripRecordingPoi)
    }

}