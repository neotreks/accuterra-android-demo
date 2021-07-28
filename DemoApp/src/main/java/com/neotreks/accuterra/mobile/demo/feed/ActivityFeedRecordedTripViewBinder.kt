package com.neotreks.accuterra.mobile.demo.feed

import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.TripListItemBinding
import com.neotreks.accuterra.mobile.demo.extensions.toUsDateString
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingStatus

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
        val binding = TripListItemBinding.bind(view)
        val data = item.data ?: return
        // Set listeners
        view.setOnClickListener { listener.onItemClick(item) }
        view.setOnLongClickListener { listener.onLongItemClick(item) }
        // Date
        val date = data.startDate.toUsDateString()
        binding.tripListItemDate.text = date
        // Name and description
        binding.tripListItemName.text = data.name
        binding.tripListItemDescription.text = data.description
        // Status
        binding.tripListItemStatus.text = data.status.getName()
        val color = if (data.status == TripRecordingStatus.QUEUED || data.status == TripRecordingStatus.UPLOADED) {
            view.context.getColor(R.color.difficulty_easy)
        } else if (data.status == TripRecordingStatus.PROCESSED) {
            view.context.getColor(R.color.difficulty_piece_of_cake)
        } else {
                view.context.getColor(R.color.difficulty_advanced)
        }
        binding.tripListItemStatus.setTextColor(color)
    }

}
