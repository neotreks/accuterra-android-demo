package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapter
import com.neotreks.accuterra.mobile.sdk.trip.model.TripSharingType


/**
 *  Adapter for list of sharing types
 */
class TripSharingTypeAdapter(context: Context) : ListItemAdapter<TripSharingType>(
    context,
    TripSharingType.values().toList(),
    TripSharingTypeListViewBinder(context)
) {

    override fun getItemId(item: TripSharingType): Long {
        return item.id.toLong()
    }

}