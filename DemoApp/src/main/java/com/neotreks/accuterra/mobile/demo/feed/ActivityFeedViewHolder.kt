package com.neotreks.accuterra.mobile.demo.feed

import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView

/**
 * View holder for the [ActivityFeedRecycleViewAdapter]
 */
class ActivityFeedViewHolder(
    private val view: View,
    context: Context,
    listener: Listener,
    lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.ViewHolder(view) {

    private val recorderTripViewBinder = ActivityFeedRecordedTripViewBinder(listener)
    private val userViewBinder = ActivityFeedTripUserViewBinder(listener)
    private val headerViewBinder = ActivityFeedTripHeaderViewBinder(context, listener)
    private val statisticsViewBinder = ActivityFeedTripStatisticsViewBinder(context, listener)
    private val thumbnailViewBinder = ActivityFeedTripThumbnailViewBinder(context, listener, lifecycleScope)
    private val ugcFooterViewBinder = ActivityFeedTripUgcFooterViewBinder(context, listener)

    fun bindView(item: ActivityFeedItem) {

        when(item.type) {
            ActivityFeedItemType.LOCAL_RECORDED_TRIP -> {
                val hItem = item as ActivityFeedRecordedTripItem
                recorderTripViewBinder.bindView(view, hItem)
            }
            ActivityFeedItemType.ONLINE_TRIP_USER -> {
                val hItem = (item as ActivityFeedTripUserItem)
                userViewBinder.bindView(view, hItem)
            }
            ActivityFeedItemType.ONLINE_TRIP_HEADER -> {
                val hItem = (item as ActivityFeedTripHeaderItem)
                headerViewBinder.bindView(view, hItem)
            }
            ActivityFeedItemType.ONLINE_TRIP_STATISTICS -> {
                val hItem = (item as ActivityFeedTripStatisticsItem)
                statisticsViewBinder.bindView(view, hItem)
            }
            ActivityFeedItemType.ONLINE_TRIP_THUMBNAIL -> {
                val hItem = (item as ActivityFeedTripThumbnailItem)
                thumbnailViewBinder.bindView(view, hItem)
            }
            ActivityFeedItemType.ONLINE_TRIP_UGC_FOOTER -> {
                val hItem = (item as ActivityFeedTripUgcFooterItem)
                ugcFooterViewBinder.bindView(view, hItem)
            }
        }

    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    interface Listener {
        fun onItemClick(item: ActivityFeedItem)
        fun onLongItemClick(item: ActivityFeedItem) : Boolean
        fun onLikeClicked(item: ActivityFeedItem)
        fun onPhotosClicked(item: ActivityFeedItem)
    }

}