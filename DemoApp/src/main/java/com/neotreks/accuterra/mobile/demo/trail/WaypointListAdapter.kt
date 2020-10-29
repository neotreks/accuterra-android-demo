package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import android.view.View
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapter
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailDriveWaypoint
import kotlinx.android.synthetic.main.poi_list_item.view.*

/**
 * Adapter for list of POIs
 */
class WaypointListAdapter(
    context: Context,
    items: MutableList<TrailDriveWaypoint>,
    displayDescription: Boolean,
    displayAction: Boolean
)
    : ListItemAdapter<TrailDriveWaypoint>(context, items, WaypointListViewBinder(context, displayDescription, displayAction)) {

    var subSelectedItemId: Long? = null
        private set

    init {
        doNotConvertViews = true
    }

    override fun getItemId(item: TrailDriveWaypoint): Long {
        return item.id
    }

    fun setDisplayDescription(displayDescription: Boolean) {
        val binder = viewBinder as WaypointListViewBinder
        binder.setDisplayDescription(displayDescription)
    }

    fun setSubSelectedId(id: Long?) {
        subSelectedItemId = id
        val binder = viewBinder as WaypointListViewBinder
        binder.setSubSelectedId(subSelectedItemId)
    }

    override fun getActionView(view: View): View? {
        return view.poi_list_item_action
    }
}