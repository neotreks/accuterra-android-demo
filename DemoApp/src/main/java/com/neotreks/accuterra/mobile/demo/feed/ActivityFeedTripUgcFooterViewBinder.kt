package com.neotreks.accuterra.mobile.demo.feed

import android.content.Context
import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.extensions.getLikeIconResource
import kotlinx.android.synthetic.main.activity_feed_online_trip_ugc_footer_item.view.*

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
        val data = item.data ?: return
        // Listeners
        view.setOnClickListener { listener.onItemClick(item) }
        view.setOnLongClickListener { listener.onLongItemClick(item) }
        // Likes listener
        view.activity_feed_online_trip_ugc_footer_likes_icon.setOnClickListener {
            listener.onLikeClicked(item)
        }
        view.activity_feed_online_trip_ugc_footer_likes_envelope.setOnClickListener {
            listener.onLikeClicked(item)
        }
        view.activity_feed_online_trip_ugc_footer_likes.setOnClickListener {
            listener.onLikeClicked(item)
        }
        // Photos listener
        view.activity_feed_online_trip_ugc_footer_photos_icon.setOnClickListener {
            listener.onPhotosClicked(item)
        }
        view.activity_feed_online_trip_ugc_footer_photos.setOnClickListener {
            listener.onPhotosClicked(item)
        }
        // Data
        view.activity_feed_online_trip_ugc_footer_likes.text = context.getString(R.string.general_likes_number, data.trip.likesCount)
        view.activity_feed_online_trip_ugc_footer_comments.text = context.getString(R.string.general_comments_number, data.trip.commentsCount)
        view.activity_feed_online_trip_ugc_footer_photos.text = context.getString(R.string.general_photos_number, data.trip.imageResourceUuids.size)
        // Like icon
        val likeIcon = getLikeIconResource(data.trip.userLike)
        view.activity_feed_online_trip_ugc_footer_likes_icon.setImageResource(likeIcon)
    }

}