package com.neotreks.accuterra.mobile.demo.feed

import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.extensions.toLocalDateTimeString
import kotlinx.android.synthetic.main.activity_feed_online_trip_user_item.view.*

/**
 * View binder for the Trip User item
 */
class ActivityFeedTripUserViewBinder(listener: ActivityFeedViewHolder.Listener) : BaseActivityFeedViewBinder(listener) {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        fun getViewResourceId(): Int {
            return R.layout.activity_feed_online_trip_user_item
        }
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun bindView(view: View, item: ActivityFeedTripUserItem) {
        val data = item.data ?: return
        // Listeners
        view.setOnClickListener { listener.onItemClick(item)}
        view.setOnLongClickListener { listener.onLongItemClick(item) }
        // Data
        view.activity_feed_trip_user_user_name.text = data.userId
        view.activity_feed_trip_user_trip_date.text = data.trip.tripStart.toLocalDateTimeString()
    }

}