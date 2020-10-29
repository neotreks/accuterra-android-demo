package com.neotreks.accuterra.mobile.demo.trip.online

import android.content.Context
import android.view.View
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleCoroutineScope
import com.bumptech.glide.Glide
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.media.ApkMediaVariant
import com.neotreks.accuterra.mobile.demo.media.ApkMediaVariantUtil
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trip.model.TripMedia
import kotlinx.android.synthetic.main.component_image_view.view.*

/**
 * View Binder for the [OnlineTripMediaFileViewAdapter]
 */
class OnlineTripMediaFileViewBinder(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val imageLayoutResource: Int = R.layout.component_image_view,
    private val mediaVariant: ApkMediaVariant
): ListItemAdapterViewBinder<TripMedia> {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val mediaService = ServiceFactory.getTripMediaService(context)

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    @LayoutRes
    override fun getViewResourceId(): Int {
        return imageLayoutResource
    }

    override fun bindView(view: View, item: TripMedia, isSelected: Boolean) {
        val options = UiUtils.getDefaultImageOptions()

        // Set the placeholder before getting the right image
        val imageView = view.image_view
        imageView.setImageResource(options.placeholderId)

        // Ge the URi Async to avoid UI freezing
        lifecycleScope.launchWhenCreated {
            val url = ApkMediaVariantUtil.getUrlForVariant(item.url, item.mediaCategoryNumber, mediaVariant)
            val uri = mediaService.getMediaFile(url).value
            Glide.with(context)
                .applyDefaultRequestOptions(options)
                .load(uri)
                .into(imageView)
        }
    }

}