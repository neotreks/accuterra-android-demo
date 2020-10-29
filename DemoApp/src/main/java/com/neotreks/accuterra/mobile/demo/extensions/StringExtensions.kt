package com.neotreks.accuterra.mobile.demo.extensions

import java.util.*

/**
 * String related extensions
 */


/**
 * Returns true if given string is not NULL nor BLANK
 */
fun String?.isNotNullNorBlank(): Boolean {
    if (this == null) {
        return false
    }
    return !this.isNullOrBlank()
}

/**
 * Converts UUID into [Long] ID
 */
fun String.uuidToLong(): Long {
    return UUID.fromString(this).mostSignificantBits and Long.MAX_VALUE
}