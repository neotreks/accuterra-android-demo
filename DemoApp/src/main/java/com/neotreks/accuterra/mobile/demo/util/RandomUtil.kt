package com.neotreks.accuterra.mobile.demo.util

import java.util.*

/**
 * Utility class to generate random data
 */
object RandomUtil {

    /**
     * Return random list generated from passed list.
     * Limit is the number of returned items.
     */
    fun <T>getRandomList(list: List<T>, maxLimit: Int): List<T> {
        val result = mutableSetOf<T>()
        var number = Random().nextInt(maxLimit)
        if (number <= 0) {
            number = 1
        }
        while (result.size < number) {
            result.add(list.random())
        }
        return result.toList()
    }

}