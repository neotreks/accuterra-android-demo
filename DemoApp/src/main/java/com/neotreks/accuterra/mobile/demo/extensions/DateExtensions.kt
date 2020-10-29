package com.neotreks.accuterra.mobile.demo.extensions

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Date related extensions
 */

fun Date?.toIsoDateString(): String {
    if (this == null) {
        return ""
    }
    val tz = TimeZone.getTimeZone("UTC")
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
    df.timeZone = tz
    return df.format(this)
}

fun Date?.toIsoDateTimeString(): String {
    if (this == null) {
        return ""
    }
    val tz = TimeZone.getTimeZone("UTC")
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
    df.timeZone = tz
    return df.format(this)
}

fun Date?.toLocalDateString(): String {
    if (this == null) {
        return ""
    }
    val tz = TimeZone.getDefault()
    val df = DateFormat.getDateInstance()
    df.timeZone = tz
    return df.format(this)
}

fun Date?.toLocalDateTimeString(): String {
    if (this == null) {
        return ""
    }
    val tz = TimeZone.getDefault()
    val df = DateFormat.getDateTimeInstance()
    df.timeZone = tz
    return df.format(this)
}

fun Date?.toUsDateString(): String {
    if (this == null) {
        return ""
    }
    val tz = TimeZone.getTimeZone("UTC")
    val df = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    df.timeZone = tz
    return df.format(this)
}
