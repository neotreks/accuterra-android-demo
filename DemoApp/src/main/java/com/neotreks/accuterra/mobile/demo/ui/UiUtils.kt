package com.neotreks.accuterra.mobile.demo.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginStart
import androidx.core.view.marginTop
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

    /**
     * Resizes passed [listView] height to display all items.
     * This is needed e.g. when the list is inside a [ScrollView]
     */
    fun resizeToFitAllItems(listView: ListView) {
        val adapter: ListAdapter = listView.adapter
            ?: return //do nothing
        //set listAdapter in loop for getting final size
        var totalHeight = 0
        val listViewWeight = getInnerWidthWeight(listView)
        for (index in 0 until adapter.count) {
            val listItem: View = adapter.getView(index, null, listView)
            if (!listItem.isVisible) {
                continue
            }
            val desiredWeight = MeasureSpec.makeMeasureSpec(listViewWeight, MeasureSpec.EXACTLY)
            // Measure list item height
            listItem.measure(desiredWeight, MeasureSpec.UNSPECIFIED)
            totalHeight += listItem.measuredHeight
        }
        // Add the divider's height
        totalHeight += (listView.dividerHeight * (adapter.count - 1)) + listView.marginTop + listView.marginBottom
        //setting listview item in adapter
        val params: ViewGroup.LayoutParams = listView.layoutParams
        params.height = totalHeight
        listView.layoutParams = params
    }

    /**
     * Calculates _pure inner width_ for given view based on its parens, margins, etc.
     */
    private fun getInnerWidthWeight(view: View): Int {
        var margins = 0
        var curView:View? = view
        var width = -1
        do {
            if (curView?.width ?: -1 > 0) {
                width = curView?.width ?: -1
                margins += curView?.marginStart ?: 0
            }
            curView = curView?.parent as? View
        } while (curView != null && width < 1)

        return if (width < 0) {
            width
        } else {
            width - (2 * margins)
        }

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