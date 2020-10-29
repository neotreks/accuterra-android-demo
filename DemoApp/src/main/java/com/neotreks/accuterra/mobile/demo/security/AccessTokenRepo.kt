package com.neotreks.accuterra.mobile.demo.security

import android.content.Context
import com.neotreks.accuterra.mobile.demo.util.AbstractPreferencesRepo
import java.util.*

/**
 * Repository for the AccessToken
 *
 * Please this is not a secure implementation since this is just a demo APP.
 */
class AccessTokenRepo : AbstractPreferencesRepo(PREF_NAME) {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        const val PREF_NAME = "AccuTerraAccess"
        const val KEY_ACCESS_TOKEN = "AccessToken"
        const val KEY_REFRESH_TOKEN = "RefreshToken"
        const val KEY_TOKEN_EXPIRE = "TokenExpire"
        const val KEY_TOKEN_TYPE = "TokenType"
        const val KEY_TOKEN_SCOPE = "TokenScope"
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun save(context: Context, access: AccuTerraAccessToken) {
        edit(context)
        setString(KEY_ACCESS_TOKEN, access.accessToken)
        setString(KEY_REFRESH_TOKEN, access.refreshToken)
        setLong(KEY_TOKEN_EXPIRE, access.expireDate.time)
        setString(KEY_TOKEN_TYPE, access.tokenType)
        setString(KEY_TOKEN_SCOPE, access.scope)
        save()
    }

    fun loadAccessToken(context: Context) : AccuTerraAccessToken? {
        loadPreferences(context)
        val accessToken = getString(KEY_ACCESS_TOKEN, null) ?: return null
        val tokenType = getString(KEY_TOKEN_TYPE, null) ?: throw IllegalStateException("Missing value for: $KEY_TOKEN_TYPE")
        val refreshToken = getString(KEY_REFRESH_TOKEN, null) ?: throw IllegalStateException("Missing value for: $KEY_REFRESH_TOKEN")
        val expireIn = getLong(KEY_TOKEN_EXPIRE, 0)
        val scope = getString(KEY_TOKEN_SCOPE, null) ?: throw IllegalStateException("Missing value for: $KEY_TOKEN_SCOPE")
        return AccuTerraAccessToken(
            accessToken,
            tokenType,
            refreshToken,
            Date(expireIn),
            scope
        )
    }

}