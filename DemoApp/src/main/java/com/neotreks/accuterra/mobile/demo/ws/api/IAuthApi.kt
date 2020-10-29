package com.neotreks.accuterra.mobile.demo.ws.api

import com.neotreks.accuterra.mobile.demo.ws.response.AuthResponse
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * API for accessing WS Authorization endpoint
 */
interface IAuthApi {

    @POST("token")
    suspend fun getNewAccessToken(
        @Query("client_id")
        clientId: String,
        @Query("client_secret")
        clientSecret: String,
        @Query("grant_type")
        grantType: String,
        @Query("user_id")
        userId: String
    ): AuthResponse?

    @POST("token")
    suspend fun refreshAccessToken(
        @Query("client_id")
        clientId: String,
        @Query("client_secret")
        clientSecret: String,
        @Query("grant_type")
        grantType: String,
        @Query("refresh_token")
        refreshToken: String
    ): AuthResponse?

}