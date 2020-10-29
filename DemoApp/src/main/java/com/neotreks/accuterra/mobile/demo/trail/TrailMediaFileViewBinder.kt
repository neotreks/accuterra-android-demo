package com.neotreks.accuterra.mobile.demo.trail

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
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailMedia
import kotlinx.android.synthetic.main.component_image_view.view.*

/**
 * View Binder for the [TrailMediaFileViewAdapter]
 */
class TrailMediaFileViewBinder(private val context: Context,
                               private val lifecycleScope: LifecycleCoroutineScope
): ListItemAdapterViewBinder<TrailMedia> {

    private val mediaService = ServiceFactory.getTrailMediaService(context)

    @LayoutRes
    override fun getViewResourceId(): Int {
        return R.layout.component_image_view
    }

    override fun bindView(view: View, item: TrailMedia, isSelected: Boolean) {
        lifecycleScope.launchWhenCreated {
            val options = UiUtils.getDefaultImageOptions()
            // Ge the URi Async to avoid UI freezing
            val url = ApkMediaVariantUtil.getUrlForVariant(item.url, item.mediaCategoryNumber, ApkMediaVariant.THUMBNAIL)
            val uri = mediaService.getMediaFile(url)
            Glide.with(context)
                .applyDefaultRequestOptions(options)
                .load(uri)
                .into(view.image_view)
        }
    }

}