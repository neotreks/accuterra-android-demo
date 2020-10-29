package com.neotreks.accuterra.mobile.demo

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

fun Activity.toast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

fun Activity.longToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}

/**
 * Return icon drawable for given resource ID
 */
fun Context.loadDrawable(@DrawableRes iconId: Int): Drawable? {
    return ContextCompat.getDrawable(this, iconId)
}