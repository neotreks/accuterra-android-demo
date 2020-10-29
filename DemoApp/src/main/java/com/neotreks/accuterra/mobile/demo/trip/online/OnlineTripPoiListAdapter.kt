package com.neotreks.accuterra.mobile.demo.trip.online

import android.content.Context
import com.neotreks.accuterra.mobile.demo.extensions.uuidToLong
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapter
import com.neotreks.accuterra.mobile.sdk.trip.model.TripPoint

/**
 * Adapter for list of [TripPoint]
 */
class OnlineTripPoiListAdapter(
    context: Context,
    pois: List<TripPoint>
) : ListItemAdapter<TripPoint>(context, pois, OnlineTripPoiListViewBinder(context)) {

    override fun getItemId(item: TripPoint): Long {
        return item.uuid.uuidToLong()
    }

}
