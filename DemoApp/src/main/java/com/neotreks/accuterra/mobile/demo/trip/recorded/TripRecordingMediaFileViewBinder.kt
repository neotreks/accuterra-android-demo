package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.view.View
import androidx.annotation.LayoutRes
import com.bumptech.glide.Glide
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ComponentImageViewBinding
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia

/**
 * View Binder for the [TripRecordingMediaFileViewAdapter]
 */
class TripRecordingMediaFileViewBinder(private val context: Context): ListItemAdapterViewBinder<TripRecordingMedia> {

    @LayoutRes
    override fun getViewResourceId(): Int {
        return R.layout.component_image_view
    }

    override fun bindView(view: View, item: TripRecordingMedia, isSelected: Boolean, isFavorite: Boolean) {
        val options = UiUtils.getDefaultImageOptions()
        val binding = ComponentImageViewBinding.bind(view)
        binding.imageViewFavorite.visibility = isFavorite.visibility
        Glide.with(context)
            .applyDefaultRequestOptions(options)
            .load(item.uri)
            .into(binding.imageView)
    }

}