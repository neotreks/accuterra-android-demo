package com.neotreks.accuterra.mobile.demo.feed

import android.content.Context
import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.extensions.getLocationLabelString
import com.neotreks.accuterra.mobile.demo.util.visibility
import kotlinx.android.synthetic.main.activity_feed_online_trip_header_item.view.*

/**
 * View binder for the Trip Header item
 */
class ActivityFeedTripHeaderViewBinder(
    private val context: Context,
    listener: ActivityFeedViewHolder.Listener
): BaseActivityFeedViewBinder(listener) {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        fun getViewResourceId(): Int {
            return R.layout.activity_feed_online_trip_header_item
        }

    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun bindView(view: View, item: ActivityFeedTripHeaderItem) {
        val data = item.data ?: return
        // Listeners
        view.setOnClickListener { listener.onItemClick(item) }
        view.setOnLongClickListener { listener.onLongItemClick(item) }
        // Data
        view.activity_feed_trip_header_name.text = data.trip.tripName
        view.activity_feed_trip_header_location.text = data.trip.location.getLocationLabelString()
        view.activity_feed_trip_header_description.text = data.trip.description

        val trailName = data.trip.trailName
        if (trailName == null) {
            view.activity_feed_trip_header_related_trail.visibility = false.visibility
        } else {
            view.activity_feed_trip_header_related_trail.visibility = true.visibility
            val trailNameText = context.getString(R.string.activity_online_trip_trail_with_name, trailName)
            view.activity_feed_trip_header_related_trail.text = trailNameText
        }
    }

}