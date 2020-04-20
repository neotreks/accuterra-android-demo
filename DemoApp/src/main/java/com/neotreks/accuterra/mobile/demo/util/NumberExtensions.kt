package com.neotreks.accuterra.mobile.demo.util

import java.math.RoundingMode

/**
 * Extensions related to numbers
 */

/**
 * Converts given double value from `meters` to `miles`
 */
fun Double.fromMetersToMiles(): Double {
    return this * 0.000621371192
}

/**
 *
 */
fun Double.roundTo(decimals: Int): Double {
    return this.toBigDecimal().setScale(decimals, RoundingMode.UP).toDouble()
}