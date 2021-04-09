package com.neotreks.accuterra.mobile.demo.extensions

import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Extensions related to numbers
 */

/**
 * Round double to given [decimals]
 */
fun Double.roundTo(decimals: Int): Double {
    return this.toBigDecimal().setScale(decimals, RoundingMode.UP).toDouble()
}

/** Converts inches into meters */
fun Float.fromInchesToMeters(): Float {
    return this / 39.37f
}

/** Converts meters into inches */
fun Float.fromMetersToInches(): Float {
    return this * 39.37f
}


/**
 * Returns human readable size for bytes value, for example 1.1 GB
 */
fun Long.bytesToHumanReadable(): String {

    fun formatSpace(d: Double): String {
        return DecimalFormat("#.##").format(d)
    }

    val Kb = (1 * 1024).toLong()
    val Mb = Kb * 1024
    val Gb = Mb * 1024
    val Tb = Gb * 1024
    val Pb = Tb * 1024
    val Eb = Pb * 1024
    if (this < Kb) return formatSpace(this.toDouble()) + " byte"
    if (this in Kb until Mb) return formatSpace(this.toDouble() / Kb) + " KB"
    if (this in Mb until Gb) return formatSpace(this.toDouble() / Mb) + " MB"
    if (this in Gb until Tb) return formatSpace(this.toDouble() / Gb) + " GB"
    if (this in Tb until Pb) return formatSpace(this.toDouble() / Tb) + " TB"
    if (this in Pb until Eb) return formatSpace(this.toDouble() / Pb) + " PB"
    return if (this >= Eb) formatSpace(this.toDouble() / Eb) + " EB" else "???"
}