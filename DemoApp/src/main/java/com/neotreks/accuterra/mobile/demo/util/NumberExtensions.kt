package com.neotreks.accuterra.mobile.demo.util

import java.math.RoundingMode

/**
 * Extensions related to numbers
 */

/**
 *
 */
fun Double.roundTo(decimals: Int): Double {
    return this.toBigDecimal().setScale(decimals, RoundingMode.UP).toDouble()
}