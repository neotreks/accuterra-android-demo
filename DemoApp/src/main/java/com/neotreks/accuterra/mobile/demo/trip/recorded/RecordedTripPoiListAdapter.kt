package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapter
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi

/**
 * Adapter for list of [TripRecordingPoi]
 */
class RecordedTripPoiListAdapter(
    context: Context,
    pois: List<TripRecordingPoi>
) : ListItemAdapter<TripRecordingPoi>(context, pois, RecordedTripPoiListViewBinder(context)) {

    override fun getItemId(item: TripRecordingPoi): Long {
        return item.pk!!
    }

}