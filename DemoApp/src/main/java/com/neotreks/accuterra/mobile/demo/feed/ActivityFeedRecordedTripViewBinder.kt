package com.neotreks.accuterra.mobile.demo.feed

import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.extensions.toUsDateString
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingStatus
import kotlinx.android.synthetic.main.trip_list_item.view.*

/**
 * View binder for the local Recorded Trip item
 */
class ActivityFeedRecordedTripViewBinder(listener: ActivityFeedViewHolder.Listener): BaseActivityFeedViewBinder(listener) {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        const val viewResourceId = R.layout.trip_list_item
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    fun getViewResourceId(): Int {
        return R.layout.trip_list_item
    }

    fun bindView(view: View, item: ActivityFeedRecordedTripItem) {
        val data = item.data ?: return
        // Set listeners
        view.setOnClickListener { listener.onItemClick(item) }
        view.setOnLongClickListener { listener.onLongItemClick(item) }
        // Date
        val date = data.startDate.toUsDateString()
        view.trip_list_item_date.text = date
        // Name and description
        view.trip_list_item_name.text = data.name
        view.trip_list_item_description.text = data.description
        // Status
        view.trip_list_item_status.text = data.status.getName()
        val color = if (data.status == TripRecordingStatus.QUEUED || data.status == TripRecordingStatus.UPLOADED) {
                view.context.getColor(R.color.colorPrimary)
        } else {
                view.context.getColor(R.color.colorAccent)
        }
        view.trip_list_item_status.setTextColor(color)
    }

}
