package com.neotreks.accuterra.mobile.demo.feed

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for list of UGC Trips
 */
class ActivityFeedRecycleViewAdapter(
    private val context: Context,
    private var items: List<ActivityFeedItem>,
    private val listener: ActivityFeedViewHolder.Listener,
    private val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.Adapter<ActivityFeedViewHolder>() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "ActivityFeedListAdapter"
    }

    init {
        setHasStableIds(false)
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityFeedViewHolder {
        val view = when (viewType) {
            ActivityFeedItemType.LOCAL_RECORDED_TRIP.ordinal -> {
                val resourceId = ActivityFeedRecordedTripViewBinder.viewResourceId
                LayoutInflater.from(parent.context).inflate(resourceId, parent, false)
            }
            ActivityFeedItemType.ONLINE_TRIP_USER.ordinal -> {
                val resourceId = ActivityFeedTripUserViewBinder.getViewResourceId()
                LayoutInflater.from(parent.context).inflate(resourceId, parent, false)
            }
            ActivityFeedItemType.ONLINE_TRIP_HEADER.ordinal -> {
                val resourceId = ActivityFeedTripHeaderViewBinder.getViewResourceId()
                LayoutInflater.from(parent.context).inflate(resourceId, parent, false)
            }
            ActivityFeedItemType.ONLINE_TRIP_STATISTICS.ordinal -> {
                val resourceId = ActivityFeedTripStatisticsViewBinder.getViewResourceId()
                LayoutInflater.from(parent.context).inflate(resourceId, parent, false)
            }
            ActivityFeedItemType.ONLINE_TRIP_THUMBNAIL.ordinal -> {
                val resourceId = ActivityFeedTripThumbnailViewBinder.getViewResourceId()
                LayoutInflater.from(parent.context).inflate(resourceId, parent, false)
            }
            ActivityFeedItemType.ONLINE_TRIP_UGC_FOOTER.ordinal -> {
                val resourceId = ActivityFeedTripUgcFooterViewBinder.getViewResourceId()
                LayoutInflater.from(parent.context).inflate(resourceId, parent, false)
            }
            else -> throw IllegalStateException("Cannot find daily weather view for: $viewType")
        }

        return ActivityFeedViewHolder(view, context, listener, lifecycleScope)
    }

    override fun onBindViewHolder(holder: ActivityFeedViewHolder, position: Int) {
        holder.bindView(getItem(position))
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemsCount = ${items.size}")
        return items.size
    }

    override fun getItemId(position: Int): Long {
        Log.d(TAG, "getItemId: $position")
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun getItem(position: Int): ActivityFeedItem {
        Log.d(TAG, "getItem: $position")
        return items.getOrNull(position)
            ?: throw IllegalArgumentException("There is no item on position: $position")
    }

    fun getItemPosition(item: ActivityFeedItem): Int {
        return items.indexOf(item)
    }

    @UiThread
    @Synchronized
    fun setItems(items: List<ActivityFeedItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun notifyItemChanged(item: ActivityFeedTripUgcFooterItem) {
        val position = getItemPosition(item)
        notifyItemChanged(position, item)
    }

}