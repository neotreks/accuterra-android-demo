package com.neotreks.accuterra.mobile.demo.extensions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.Editable
import android.view.View
import androidx.annotation.DrawableRes
import com.google.android.material.snackbar.Snackbar
import com.neotreks.accuterra.mobile.demo.BuildConfig
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

/**
 * Returns trimmed string value or null if given string is null or blank
 */
fun Editable?.trimOrNull(): String? {
    if (this.isNullOrBlank()) {
        return null
    }
    return this.toString().trim()
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

/**
 * Utility to add `Settings` action to the snackbar
 */
fun Snackbar.addSettingsAction(activity: Activity): Snackbar {
    this.setAction(R.string.general_settings) {
        // Build intent that displays the App settings screen.
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri: Uri = Uri.fromParts(
            "package",
            BuildConfig.APPLICATION_ID, null
        )
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
    }
    return this
}