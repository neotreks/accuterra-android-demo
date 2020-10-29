package com.neotreks.accuterra.mobile.demo.extensions

import android.view.View
import androidx.annotation.DrawableRes
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.util.OnSingleClickListener

/**
 * UI related extensions
 */


/**
 * Sets visibility to [View.visible]
 */
fun View.visible() {
    this.visibility = View.VISIBLE
}

/**
 * Sets visibility to [View.gone]
 */
fun View.gone() {
    this.visibility = View.GONE
}

/**
 * Allow to set the `single click listener` that prevents multiple clicks in a short time
 */
fun View.setOnSingleClickListener(onClicked: (view:View?)->(Unit), interval:Long = 1000L) {
    this.setOnClickListener(OnSingleClickListener(onClicked = onClicked, intervalMs = interval))
}

@DrawableRes
fun getLikeIconResource(hasLiked: Boolean) : Int {
    return if (hasLiked) {
            R.drawable.ic_thumb_up_alt_blue_24dp
        } else {
            R.drawable.ic_thumb_up_alt_black_24dp
        }
}

@DrawableRes
fun getFavoriteIconResource(hasLiked: Boolean) : Int {
    return if (hasLiked) {
        R.drawable.ic_bookmark_turned_in_black_24dp
    } else {
        R.drawable.ic_bookmark_border_24px
    }
}