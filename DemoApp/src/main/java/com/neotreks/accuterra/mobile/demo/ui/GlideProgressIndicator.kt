package com.neotreks.accuterra.mobile.demo.ui

import android.widget.ProgressBar
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.neotreks.accuterra.mobile.demo.util.visibility

/**
 * Utility class that hides progress indicator when image is loaded or loads fails
 */
class GlideProgressIndicator(private val progressBar: ProgressBar) : RequestListener<Any> {

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<Any>?,
        isFirstResource: Boolean
    ): Boolean {
        progressBar.visibility = false.visibility
        return false
    }

    override fun onResourceReady(
        resource: Any?,
        model: Any?,
        target: Target<Any>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
    ): Boolean {
        progressBar.visibility = false.visibility
        return false
    }

}