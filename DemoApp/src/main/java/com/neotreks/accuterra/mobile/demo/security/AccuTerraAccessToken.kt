package com.neotreks.accuterra.mobile.demo.security

import java.util.*

/**
 * Access token for AccuTerra Services
 */
data class AccuTerraAccessToken (

    val accessToken: String,

    val tokenType: String,

    val refreshToken: String,

    val expireDate: Date,

    val scope: String

) {

    /**
     * Return true if the token does not expire in next 60 seconds.
     */
    fun isValid() : Boolean {
        return (expireDate.time - (System.currentTimeMillis() + (60 * 1_000))) > 0
    }

}