package com.neotreks.accuterra.mobile.demo.ws

import okhttp3.OkHttpClient

/**
 * APK's shared instance of the [OkHttpClient]
 */
object DemoOkHttpClientShared {

    val instance = OkHttpClient()

}