package com.neotreks.accuterra.mobile.demo.extensions

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type.captionBar
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsCompat.Type.statusBars
import androidx.core.view.WindowInsetsCompat.Type.systemBars

/**
 * Apply ALL window insets to the root view of the activity
 */
fun applyAllWindowInsets(root: View) {
    applySystemBarsInsets(root, systemBars())
}

fun applyAllWindowInsetsButStatusBar(view: View) {
    applySystemBarsInsets(view, navigationBars() or captionBar())
}

fun applyStatusBarWindowInsets(view: View) {
    applySystemBarsInsets(view, statusBars())
}

private fun applySystemBarsInsets(view: View, insetsMask: Int) {
    ViewCompat.setOnApplyWindowInsetsListener(view) { view, insets ->
        val systemBars = insets.getInsets(insetsMask)
        view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
        insets
    }
}