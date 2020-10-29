package com.neotreks.accuterra.mobile.demo.ws.api

import com.neotreks.accuterra.mobile.demo.BuildConfig
import com.neotreks.accuterra.mobile.demo.extensions.createRetrofit
import com.neotreks.accuterra.mobile.demo.ws.response.AuthResponse

/**
 * API for accessing WS Authorization endpoint
 */
class AuthApi: IAuthApi {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val api: IAuthApi

    /* * * * * * * * * * * * */
    /*      CONSTRUCTOR      */
    /* * * * * * * * * * * * */

    init {
        val timeout: Long = 30 // seconds
        val apiUrl = BuildConfig.WS_AUTH_URL
        val retrofit = createRetrofit(apiUrl, timeout)
        api = retrofit.create(IAuthApi::class.java)
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override suspend fun getNewAccessToken(
        clientId: String,
        clientSecret: String,
        grantType: String,
        userId: String
    ): AuthResponse? {
        return api.getNewAccessToken(clientId, clientSecret, grantType, userId)
    }

    override suspend fun refreshAccessToken(
        clientId: String,
        clientSecret: String,
        grantType: String,
        refreshToken: String
    ): AuthResponse? {
        return api.refreshAccessToken(clientId, clientSecret, grantType, refreshToken)
    }

}