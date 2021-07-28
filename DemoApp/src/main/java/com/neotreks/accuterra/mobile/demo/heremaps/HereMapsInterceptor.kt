package com.neotreks.accuterra.mobile.demo.heremaps

import com.neotreks.accuterra.mobile.demo.BuildConfig
import com.neotreks.accuterra.mobile.sdk.IMapRequestInterceptor
import okhttp3.Interceptor
import okhttp3.Response

/**
 * HERE Maps map tile request interceptor
 */
class HereMapsInterceptor: IMapRequestInterceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (request.url.host.endsWith("maps.api.here.com", ignoreCase = true)) {
            val urlBuilder = request.url.newBuilder()
            urlBuilder.addEncodedQueryParameter("app_id", BuildConfig.HERE_APPID)
            urlBuilder.addEncodedQueryParameter("app_code", BuildConfig.HERE_APPCODE)

            request = request.newBuilder()
                .url(urlBuilder.build())
                .build()
        }
        return chain.proceed(request)
    }
}