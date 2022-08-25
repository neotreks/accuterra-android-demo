package com.neotreks.accuterra.mobile.demo.security

import android.content.Context
import androidx.annotation.Keep
import com.neotreks.accuterra.mobile.demo.BuildConfig
import com.neotreks.accuterra.mobile.sdk.ICredentialsAccessProvider
import com.neotreks.accuterra.mobile.sdk.ITokenAccessProvider
import com.neotreks.accuterra.mobile.sdk.SdkManager
import com.neotreks.accuterra.mobile.sdk.security.model.ClientCredentials

/**
 * Class for managing access to AccuTerra services using credentials
 */
@Keep
class DemoCredentialsAccessManager : ICredentialsAccessProvider {

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override suspend fun getClientCredentials(context: Context): ClientCredentials {
        return ClientCredentials(
            clientId = BuildConfig.WS_AUTH_CLIENT_ID,
            clientSecret = BuildConfig.WS_AUTH_CLIENT_SECRET
        )
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    suspend fun resetToken(context: Context) {
        SdkManager.resetAccessToken(context)
    }

}

/**
 * Class for managing access to AccuTerra services by providing the token
 */
@Keep
class DemoTokenAccessManager : ITokenAccessProvider {

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override suspend fun getAccessToken(context: Context): String? {
        throw NotImplementedError("Implement custom token provider here")
    }

    override suspend fun refreshAccessToken(context: Context): String? {
        throw NotImplementedError("Implement token refresh here")
    }
}
