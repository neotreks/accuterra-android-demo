package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapter
import com.neotreks.accuterra.mobile.sdk.trail.model.PointType


/**
 *  Adapter for list of point types
 */
class PointTypeAdapter(context: Context, types: List<PointType>) : ListItemAdapter<PointType>(
    context,
    types,
    PointTypeListViewBinder(context)
) {

    override fun getItemId(item: PointType): Long {
        return item.code.hashCode().toLong()
    }

}