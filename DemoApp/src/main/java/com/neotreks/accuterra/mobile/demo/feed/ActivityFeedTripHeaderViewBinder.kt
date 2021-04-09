package com.neotreks.accuterra.mobile.demo.feed

import android.content.Context
import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityFeedOnlineTripHeaderItemBinding
import com.neotreks.accuterra.mobile.demo.extensions.getLocationLabelString
import com.neotreks.accuterra.mobile.demo.util.visibility

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
        val binding = ActivityFeedOnlineTripHeaderItemBinding.bind(view)
        val data = item.data ?: return
        // Listeners
        view.setOnClickListener { listener.onItemClick(item) }
        view.setOnLongClickListener { listener.onLongItemClick(item) }
        // Data
        binding.activityFeedTripHeaderName.text = data.trip.tripName
        binding.activityFeedTripHeaderLocation.text = data.trip.location.getLocationLabelString()
        binding.activityFeedTripHeaderDescription.text = data.trip.description

        val trailName = data.trip.trailName
        if (trailName == null) {
            binding. activityFeedTripHeaderRelatedTrail.visibility = false.visibility
        } else {
            binding. activityFeedTripHeaderRelatedTrail.visibility = true.visibility
            val trailNameText = context.getString(R.string.activity_online_trip_trail_with_name, trailName)
            binding. activityFeedTripHeaderRelatedTrail.text = trailNameText
        }
    }

}