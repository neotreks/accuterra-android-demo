package com.neotreks.accuterra.mobile.demo

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.neotreks.accuterra.mobile.demo.media.ApkMediaVariant
import com.neotreks.accuterra.mobile.demo.media.ApkMediaVariantUtil
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailMedia
import com.neotreks.accuterra.mobile.sdk.trail.service.ITrailMediaService
import kotlinx.android.synthetic.main.image_carousel_item.view.*
import java.net.URL

/**
 * Trail Media Adapter
 */
class TrailMediaAdapter(private val mediaList: List<TrailMedia>,
                        private val mediaService: ITrailMediaService,
                        private val lifecycleScope: LifecycleCoroutineScope,
                        private val clickListener: TrailMediaClickListener,
                        private val context: Context
) :
    RecyclerView.Adapter<TrailMediaAdapter.MyViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder.
    // Each data item is just a string in this case that is shown in a TextView.
    class MyViewHolder(frameLayout: FrameLayout)
        : RecyclerView.ViewHolder(frameLayout) {

        var url: URL? = null
        val imageView: AppCompatImageView = frameLayout.image_carousel_item_image
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): MyViewHolder {
        // create a new view
        val frameLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.image_carousel_item, parent, false) as FrameLayout
        // set the view's size, margins, padding and layout parameters
        return MyViewHolder(
            frameLayout
        )
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        lifecycleScope.launchWhenResumed {

            val options =
                UiUtils.getDefaultImageOptions()
            // Display default image first
            holder.imageView.setImageResource(options.placeholderId)
            val media = mediaList[position]
            // We need to get url for the thumbnail variant
            val url = ApkMediaVariantUtil.getUrlForVariant(media.url, media.mediaCategoryNumber, ApkMediaVariant.THUMBNAIL)
            // Get the Uri from the media service
            val uri = mediaService.getMediaFile(url)
            // Click listener
            holder.imageView.setOnClickListener {
                clickListener.onMediaClicked(
                    media
                )
            }
            // Display image from the Uri
            Glide.with(context)
                .applyDefaultRequestOptions(options)
                .load(uri)
                .into(holder.imageView)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = mediaList.size
}

/**
 * Media click listener
 */
interface TrailMediaClickListener {
    fun onMediaClicked(media: TrailMedia)
}