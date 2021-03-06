package com.neotreks.accuterra.mobile.demo.feed

import android.content.Context
import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityFeedOnlineTripStatisticsItemBinding
import com.neotreks.accuterra.mobile.demo.settings.Formatter

/**
 * View binder for the Trip Statistics item
 */
class ActivityFeedTripStatisticsViewBinder(
    private val context: Context,
    listener: ActivityFeedViewHolder.Listener) :
BaseActivityFeedViewBinder(listener) {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val distanceFormatter = Formatter.getDistanceFormatter()
    private val drivingTimeFormatter = Formatter.getDrivingTimeFormatter()

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        fun getViewResourceId(): Int {
            return R.layout.activity_feed_online_trip_statistics_item
        }
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun bindView(view: View, item: ActivityFeedTripStatisticsItem) {
        val data = item.data ?: return
        // Listeners
        view.setOnClickListener { listener.onItemClick(item) }
        view.setOnLongClickListener { listener.onLongItemClick(item) }
        // Data
        val binding = ActivityFeedOnlineTripStatisticsItemBinding.bind(view)
        binding.activityFeedOnlineTripStatisticsDistance.text = distanceFormatter.formatDistance(context, data.trip.statistics.length)
        binding.activityFeedOnlineTripStatisticsDuration.text = drivingTimeFormatter.formatDrivingTime(context, data.trip.statistics.drivingTime)
        binding.activityFeedOnlineTripStatisticsType.text = if (data.trip.trailId == null) {
            context.getString(R.string.activity_feed_trip_type_free_roam)
        } else {
            context.getString(R.string.activity_feed_trip_type_trail_drive)
        }
    }

}