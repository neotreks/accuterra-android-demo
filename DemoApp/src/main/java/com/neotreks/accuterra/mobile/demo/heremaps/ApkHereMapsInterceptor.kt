package com.neotreks.accuterra.mobile.demo.heremaps

import com.neotreks.accuterra.mobile.demo.BuildConfig
import com.neotreks.accuterra.mobile.sdk.map.util.HereMapsInterceptor

/**
 * APK implementation of HERE maps request interceptor
 */
class ApkHereMapsInterceptor: HereMapsInterceptor() {

    override fun getApiKey(): String {
        return BuildConfig.HERE_MAPS_API_KEY
    }

}