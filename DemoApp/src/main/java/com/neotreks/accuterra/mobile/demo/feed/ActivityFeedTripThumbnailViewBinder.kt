package com.neotreks.accuterra.mobile.demo.feed

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import com.bumptech.glide.Glide
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityFeedOnlineTripThumbnailItemBinding
import com.neotreks.accuterra.mobile.demo.media.ApkMediaVariant
import com.neotreks.accuterra.mobile.demo.media.ApkMediaVariantUtil
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.demo.util.CrashSupport
import com.neotreks.accuterra.mobile.sdk.ServiceFactory

/**
 * View binder for the Trip Thumbnail item
 */
class ActivityFeedTripThumbnailViewBinder(
    private val context: Context,
    listener: ActivityFeedViewHolder.Listener,
    private val lifecycleScope: LifecycleCoroutineScope,
) : BaseActivityFeedViewBinder(listener) {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val mediaService = ServiceFactory.getTripMediaService(context)

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "ActFeedTripThumbnailVB"
        fun getViewResourceId(): Int {
            return R.layout.activity_feed_online_trip_thumbnail_item
        }
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun bindView(view: View, item: ActivityFeedTripThumbnailItem) {
        val data = item.data ?: return
        // Listeners
        view.setOnClickListener { listener.onItemClick(item) }
        view.setOnLongClickListener { listener.onLongItemClick(item) }
        val options = UiUtils.getDefaultImageOptions()

        // Set the placeholder before getting the right image
        val binding =  ActivityFeedOnlineTripThumbnailItemBinding.bind(view)
        val imageView = binding.activityFeedOnlineTripThumbnailMap
        imageView.setImageResource(options.placeholderId)

        lifecycleScope.launchWhenCreated {
            // Ge the Uri Async to avoid UI freezing
            var uri: Uri? = null
            data.trip.mapImage?.let { mapImageDef->
                val url = ApkMediaVariantUtil.getUrlForVariant(mapImageDef.url, mapImageDef.mediaCategoryNumber,
                    ApkMediaVariant.DEFAULT)
                val result = mediaService.getMediaFile(url)
                if (result.isSuccess) {
                    uri = result.value!!
                } else {
                    Log.e(TAG, "Error while binding a view: ${result.buildErrorMessage()}",
                        result.error)
                    CrashSupport.reportError(result)
                }
            }
            if (uri == null) {
                imageView.setImageResource(options.errorId)
            } else {
                // Display the thumbnail
                Glide.with(context)
                    .applyDefaultRequestOptions(options)
                    .load(uri)
                    .into(imageView)
            }
        }

    }

}