package com.neotreks.accuterra.mobile.demo.util

import android.text.InputFilter

/**
 * Input dialog display options
 */
data class InputDialogOptions(

    var minInputLength: Int = 1,

    var maxInputLength: Int? = null,

    var inputFilters : Array<InputFilter>? = null,

    /**
     * Number of displayed lines
     */
    var lines: Int? = null

)