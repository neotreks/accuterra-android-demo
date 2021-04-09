package com.neotreks.accuterra.mobile.demo.ui

import android.text.InputFilter
import android.text.Spanned

/**
 * Filter providing input filter based on regular expression.
 */
open class RegExInputFilter(regexString: String) : InputFilter {

    private val regex : Regex = Regex(regexString)

    override fun filter(source: CharSequence?, start: Int, end: Int, dest: Spanned?, dstart: Int, dend: Int): CharSequence? {

        val inputString = (dest.toString() + source.toString())
        if (regex.matches(inputString)) {
            return null
        }

        return ""
    }

}