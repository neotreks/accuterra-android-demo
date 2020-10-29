package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapter
import com.neotreks.accuterra.mobile.sdk.trail.model.PointSubtype


/**
 *  Adapter for list of pint types
 */
class PointSubtypeAdapter(context: Context, subtypes: List<PointSubtype>) : ListItemAdapter<PointSubtype>(
    context,
    subtypes,
    PointSubtypeListViewBinder(context)
) {

    override fun getItemId(item: PointSubtype): Long {
        return item.code.hashCode().toLong()
    }

}