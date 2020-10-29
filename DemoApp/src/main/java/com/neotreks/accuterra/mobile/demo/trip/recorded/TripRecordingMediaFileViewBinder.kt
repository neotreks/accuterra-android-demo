package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.view.View
import androidx.annotation.LayoutRes
import com.bumptech.glide.Glide
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import kotlinx.android.synthetic.main.component_image_view.view.*

/**
 * View Binder for the [TripRecordingMediaFileViewAdapter]
 */
class TripRecordingMediaFileViewBinder(private val context: Context): ListItemAdapterViewBinder<TripRecordingMedia> {

    @LayoutRes
    override fun getViewResourceId(): Int {
        return R.layout.component_image_view
    }

    override fun bindView(view: View, item: TripRecordingMedia, isSelected: Boolean) {
        val options = UiUtils.getDefaultImageOptions()
        Glide.with(context)
            .applyDefaultRequestOptions(options)
            .load(item.uri)
            .into(view.image_view)
    }

}