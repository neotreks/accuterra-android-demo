package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.view.View
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleCoroutineScope
import com.bumptech.glide.Glide
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ComponentImageViewBinding
import com.neotreks.accuterra.mobile.demo.media.ApkMediaVariant
import com.neotreks.accuterra.mobile.demo.media.ApkMediaVariantUtil
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia

/**
 * View Binder for the [TripRecordingMediaFileViewAdapter]
 */
class TripRecordingMediaFileViewBinder(
    context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
): ListItemAdapterViewBinder<TripRecordingMedia> {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val context = context.applicationContext

    private val mediaService = ServiceFactory.getTripMediaService(this.context)

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    @LayoutRes
    override fun getViewResourceId(): Int {
        return R.layout.component_image_view
    }

    override fun bindView(view: View, item: TripRecordingMedia, isSelected: Boolean, isFavorite: Boolean) {
        val options = UiUtils.getDefaultImageOptions()
        val binding = ComponentImageViewBinding.bind(view)
        if (item.isLocalMedia) {
            // This is case when new Media are recorded and are available locally
            // So we display it via `TripRecordingMedia.uri`
            binding.imageViewFavorite.visibility = isFavorite.visibility
            Glide.with(context)
                .applyDefaultRequestOptions(options)
                .load(item.uri)
                .into(binding.imageView)
        } else {
            // This is an online Trip Editing case when given _original_ media
            // are not available locally but are present on the sever.
            // We have to display it the same way we do for `Online Trip Media`
            lifecycleScope.launchWhenCreated {
                val baseUrl = item.serverMedialUrl ?: return@launchWhenCreated
                val mediaCategoryNumber = item.serverMediaCategoryNumber ?: return@launchWhenCreated
                val url = ApkMediaVariantUtil.getUrlForVariant(baseUrl, mediaCategoryNumber, ApkMediaVariant.DEFAULT)
                val uri = mediaService.getMediaFile(url).value
                binding.imageViewFavorite.visibility = isFavorite.visibility
                Glide.with(context)
                    .applyDefaultRequestOptions(options)
                    .load(uri)
                    .into(binding.imageView)
            }
        }
    }

}