package com.neotreks.accuterra.mobile.demo.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.TextView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.neotreks.accuterra.mobile.demo.BuildConfig
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.loadDrawable
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption

/**
 * UI related utility methods
 */
object UiUtils {


    /**
     * Returns corresponding [Drawable] for given [trackingOption].
     */
    fun getLocationTrackingIcon(trackingOption: TrackingOption, context: Context): Drawable {
        val iconResource = when(trackingOption) {
            TrackingOption.NONE,
            TrackingOption.NONE_WITH_LOCATION,
            TrackingOption.NONE_WITH_GPS_LOCATION -> {
                R.drawable.ic_location_disabled_24px
            }
            TrackingOption.LOCATION -> {
                R.drawable.ic_my_location_24px
            }
            TrackingOption.DRIVING -> {
                R.drawable.ic_location_driving_24px
            }
            TrackingOption.NONE_WITH_DRIVING -> {
                R.drawable.ic_location_driving_disabled_24px
            }
            else -> {
                throw NotImplementedError("The $trackingOption tracking option is not supported.")
            }
        }
        return context.loadDrawable(iconResource)!!
    }

    /**
     * Returns next element from given array of [options] based on the [current] element.
     */
    fun <T>loopNextElement(current: T, options: Array<T>): T {
        val currentIndex = options.indexOf(current)
        var nextIndex = currentIndex + 1
        if (nextIndex == options.size) {
            nextIndex = 0
        }
        return options[nextIndex]
    }

    /**
     * Returns default display options for Glide
     */
    fun getDefaultImageOptions(): RequestOptions {
        return RequestOptions()
            .placeholder(R.drawable.ic_image_gray_24px)
            .error(R.drawable.ic_broken_image_gray_24dp)
            .fallback(android.R.drawable.ic_dialog_alert)
            .diskCacheStrategy(DiskCacheStrategy.NONE) // No disc caching for now
            .skipMemoryCache(true)  // No memory caching for now
            .fitCenter()
    }

    fun setApkVersionText(textView: TextView) {
        textView.text = BuildConfig.VERSION_NAME
    }

}

fun Float.fromDpToPixels(context: Context): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        context.resources.displayMetrics
    )
}

fun Int.fromDpToPixels(context: Context): Float {
    return this.toFloat().fromDpToPixels(context)
}