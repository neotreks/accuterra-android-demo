package com.neotreks.accuterra.mobile.demo.util

import android.text.Editable
import android.text.TextWatcher

/**
 * Text change listener
 */
class TextChangeListener(private val onValueChanged: ((newValue: String) -> Unit)?) : TextWatcher {

    override fun afterTextChanged(s: Editable?) {
        onValueChanged?.invoke(s.toString())
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }
}