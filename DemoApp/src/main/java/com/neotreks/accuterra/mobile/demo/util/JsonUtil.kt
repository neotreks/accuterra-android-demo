package com.neotreks.accuterra.mobile.demo.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Utilities for JSON related functionality
 */
class JsonUtil {
    companion object {
        fun buildStandardGson(): Gson {
            return GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                .setLenient()
                .create()
        }
    }
}

/**
 * A general method to convert class into the JSON
 */
internal fun Any.toJson(): String {
    val gson = JsonUtil.buildStandardGson()
    return gson.toJson(this)
}

/**
 * A general method to convert JSON into a class
 */
internal fun <T>String.jsonToClass(clazz: Class<T>): T {
    val gson = JsonUtil.buildStandardGson()
    return gson.fromJson(this, clazz)
}