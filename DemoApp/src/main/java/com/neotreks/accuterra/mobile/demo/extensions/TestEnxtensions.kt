package com.neotreks.accuterra.mobile.demo.extensions

import kotlin.random.Random

/**
 * Test related extensions
 */


fun randomByte(random: Random = Random, start: Byte = Byte.MIN_VALUE, end: Byte = Byte.MAX_VALUE): Byte {
    return random.nextInt(start.toInt(), end.toInt()).toByte()
}

fun randomShort(random: Random = Random, start: Short = Short.MIN_VALUE, end: Short = Short.MAX_VALUE): Short {
    return random.nextInt(start.toInt(), end.toInt()).toShort()
}

fun randomInt(random: Random = Random, start: Int = Int.MIN_VALUE, end: Int = Int.MAX_VALUE): Int {
    return random.nextInt(start, end)
}

fun randomLong(random: Random = Random, start: Long = Long.MIN_VALUE, end: Long = Long.MAX_VALUE): Long {
    return random.nextLong(start, end)
}

fun randomFloat(random: Random = Random, start: Float = Float.MIN_VALUE, end: Float = Float.MAX_VALUE): Float {
    return random.nextDouble(start.toDouble(), end.toDouble()).toFloat()
}

fun randomDouble(random: Random = Random, start: Double = Double.MIN_VALUE, end: Double = Double.MAX_VALUE): Double {
    return random.nextDouble(start, end)
}

private  val allowedRandomChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
fun randomText(random: Random = Random, length: Int): String {
    return (1..length)
        .map { allowedRandomChars.random(random) }
        .joinToString("")
}

fun randomBoolean(random: Random = Random): Boolean {
    return random.nextBoolean()
}