package com.neotreks.accuterra.mobile.demo.util

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.neotreks.accuterra.mobile.demo.R


/**
 * UI related extension methods
 */

fun TextView.drawUpArrowOnLeftSide() =
    this.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_keyboard_arrow_up_black_24dp,0,0,0)

fun TextView.drawDownArrowOnLeftSide() =
    this.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_keyboard_arrow_down_black_24dp,0,0,0)

val Boolean.visibility: Int
    get() {
        return if (this) View.VISIBLE else View.GONE
    }

fun Context.showSoftKeyBoard(view: View?) {
    val imm: InputMethodManager =
        ContextCompat.getSystemService(this, InputMethodManager::class.java)!!
    if (view == null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        } else {
            view.postDelayed(
            {
                view.requestFocus()
                imm.showSoftInput(view, 0)
            }, 150)
        }
}

fun Context.hideSoftKeyboard(view: View?) {
    if (view == null) return
    val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}