package com.neotreks.accuterra.mobile.demo.util

import android.widget.TextView
import com.neotreks.accuterra.mobile.demo.R

/**
 * UI related extension methods
 */

fun TextView.drawUpArrowOnLeftSide() =
    this.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_keyboard_arrow_up_black_24dp,0,0,0)

fun TextView.drawDownArrowOnLeftSide() =
    this.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_keyboard_arrow_down_black_24dp,0,0,0)