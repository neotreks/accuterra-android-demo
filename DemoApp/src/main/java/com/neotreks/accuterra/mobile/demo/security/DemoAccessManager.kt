package com.neotreks.accuterra.mobile.demo.security

import android.content.Context
import androidx.annotation.Keep
import com.neotreks.accuterra.mobile.demo.BuildConfig
import com.neotreks.accuterra.mobile.demo.user.DemoIdentityManager
import com.neotreks.accuterra.mobile.demo.ws.api.AuthApi
import com.neotreks.accuterra.mobile.demo.ws.response.AuthResponse
import com.neotreks.accuterra.mobile.sdk.IAccessProvider
import org.json.JSONObject
import java.util.*

/**
 * Class for managing access to AccuTerra services
 */
@Keep
class DemoAccessManager : IAccessProvider {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val identityManager = DemoIdentityManager()

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override suspend fun getToken(context: Context): String {
        return getAccessToken(context).accessToken
    }

    override suspend fun getAccuterraMapApiKey(context: Context): String? {
        return BuildConfig.ACCUTERRA_MAP_API_KEY
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    suspend fun getAccessToken(context: Context): AccuTerraAccessToken {

        if (!BuildConfig.USE_AUTH) {
            return AccuTerraAccessToken("","","",Date(),"")
        }

        val repo = AccessTokenRepo()
        var token = repo.loadAccessToken(context)

        // We need to get new token
        if (token == null) {
            token = getNewAccessToken(context)
                ?: throw IllegalStateException("Cannot get new token.")
            repo.save(context, token)
            return token
        }

        // The token is valid
        if (token.isValid()) {
            return token
        }

        // We need to refresh the existing token
        val refreshedToken = getRefreshedToken(token)
            ?: getNewAccessToken(context)
            ?: throw IllegalStateException("Cannot refresh token")
        // Save it into the repo
        repo.save(context, refreshedToken)

        return refreshedToken
    }

    suspend fun resetToken(context: Context) {
        val repo = AccessTokenRepo()
        // Delete current token
        repo.deletePreferences(context)
        // Grab new token from the server
        getAccessToken(context)
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private suspend fun getNewAccessToken(context: Context): AccuTerraAccessToken? {

        val api = AuthApi()
        val wsToken = api.getNewAccessToken(
            clientId = BuildConfig.WS_AUTH_CLIENT_ID,
            clientSecret = BuildConfig.WS_AUTH_CLIENT_SECRET,
            grantType = "client_credentials",
            userId = identityManager.getUserId(context)
        )

        return wsToken.toApi()
    }

    private suspend fun getRefreshedToken(oldToken: AccuTerraAccessToken): AccuTerraAccessToken? {

        val api = AuthApi()
        val wsToken = api.refreshAccessToken(
            clientId = BuildConfig.WS_AUTH_CLIENT_ID,
            clientSecret = BuildConfig.WS_AUTH_CLIENT_SECRET,
            grantType = "refresh_token",
            refreshToken = oldToken.refreshToken
        )
        return wsToken.toApi()
    }

    private fun getExpirationDate(token: String) : Date {
        val parts = token.split(".")
        if (parts.count() > 2) {
            val decoded = String(android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT))
            val jwtInfo = JSONObject(decoded)
            val exp = jwtInfo["exp"] as Int
            return Date(exp * 1000L)
        } else {
            throw IllegalStateException("Cannot parse token expiration, invalid JWT token")
        }
    }

    private fun AuthResponse?.toApi(): AccuTerraAccessToken? {
        if (this == null) {
            return null
        }
        val expireDate = getExpirationDate(accessToken)
        return AccuTerraAccessToken(
            accessToken, tokenType, refreshToken, expireDate, scope
        )
    }
}
