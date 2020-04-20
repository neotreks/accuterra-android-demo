package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import com.neotreks.accuterra.mobile.sdk.trail.model.MapPoint

/**
 * Adapter for list of POIs
 */
class PoiListAdapter(context: Context, items: List<MapPoint>)
    : ListItemAdapter<MapPoint>(context, items, PoiListViewBinder(context)) {

    override fun getItemId(item: MapPoint): Long {
        return item.id
    }
}