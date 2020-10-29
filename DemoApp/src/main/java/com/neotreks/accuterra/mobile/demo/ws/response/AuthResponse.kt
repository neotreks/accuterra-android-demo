package com.neotreks.accuterra.mobile.demo.ws.response

import com.google.gson.annotations.SerializedName

/**
 * Authorization response
 */
data class AuthResponse(

    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("refresh_token")
    val refreshToken: String,

    @SerializedName("expires_in")
    val expiresIn: Int,

    @SerializedName("scope")
    val scope: String

)