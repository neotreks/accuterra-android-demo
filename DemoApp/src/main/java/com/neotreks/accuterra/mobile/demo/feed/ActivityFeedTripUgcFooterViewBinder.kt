package com.neotreks.accuterra.mobile.demo.feed

import android.content.Context
import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityFeedOnlineTripUgcFooterItemBinding
import com.neotreks.accuterra.mobile.demo.extensions.getLikeIconResource

/**
 * View binder for the Trip UGC Footer item
 */
class ActivityFeedTripUgcFooterViewBinder(
    private val context: Context,
    listener: ActivityFeedViewHolder.Listener
) : BaseActivityFeedViewBinder(listener) {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        fun getViewResourceId(): Int {
            return R.layout.activity_feed_online_trip_ugc_footer_item
        }
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun bindView(view: View, item: ActivityFeedTripUgcFooterItem) {
        val binding = ActivityFeedOnlineTripUgcFooterItemBinding.bind(view)
        val data = item.data ?: return
        // Listeners
        view.setOnClickListener { listener.onItemClick(item) }
        view.setOnLongClickListener { listener.onLongItemClick(item) }
        // Likes listener
        binding.activityFeedOnlineTripUgcFooterLikesIcon.setOnClickListener {
            listener.onLikeClicked(item)
        }
        binding.activityFeedOnlineTripUgcFooterLikesEnvelope.setOnClickListener {
            listener.onLikeClicked(item)
        }
        binding.activityFeedOnlineTripUgcFooterLikes.setOnClickListener {
            listener.onLikeClicked(item)
        }
        // Photos listener
        binding.activityFeedOnlineTripUgcFooterPhotosIcon.setOnClickListener {
            listener.onPhotosClicked(item)
        }
        binding.activityFeedOnlineTripUgcFooterPhotos.setOnClickListener {
            listener.onPhotosClicked(item)
        }
        // Data
        binding.activityFeedOnlineTripUgcFooterLikes.text = context.getString(R.string.general_likes_number, data.trip.likesCount)
        binding.activityFeedOnlineTripUgcFooterComments.text = context.getString(R.string.general_comments_number, data.trip.commentsCount)
        binding.activityFeedOnlineTripUgcFooterPhotos.text = context.getString(R.string.general_photos_number, data.trip.imageResourceUuids.size)
        // Like icon
        val likeIcon = getLikeIconResource(data.trip.userLike)
        binding.activityFeedOnlineTripUgcFooterLikesIcon.setImageResource(likeIcon)
    }

}