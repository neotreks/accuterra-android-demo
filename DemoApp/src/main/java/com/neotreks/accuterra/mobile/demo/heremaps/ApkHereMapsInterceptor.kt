package com.neotreks.accuterra.mobile.demo.heremaps

import com.neotreks.accuterra.mobile.demo.BuildConfig
import com.neotreks.accuterra.mobile.sdk.map.util.HereMapsInterceptor

/**
 * APK implementation of HERE maps request interceptor
 */
class ApkHereMapsInterceptor: HereMapsInterceptor() {

    override fun getAppId(): String {
        return BuildConfig.HERE_APPID
    }

    override fun getAppCode(): String {
        return BuildConfig.HERE_APPCODE
    }

}