package com.neotreks.accuterra.mobile.demo.extensions

import com.neotreks.accuterra.mobile.demo.util.JsonUtil
import com.neotreks.accuterra.mobile.demo.ws.DemoOkHttpClientShared
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


/**
 * Creates instance of the Retrofit interface
 */
internal fun createRetrofit(
    baseUrl: String,
    readTimeoutSeconds: Long
): Retrofit {

    val okHttpClientBuilder = DemoOkHttpClientShared.instance.newBuilder()

    okHttpClientBuilder
        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)

    val gson = JsonUtil.buildStandardGson()

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClientBuilder.build())
        .build()

}