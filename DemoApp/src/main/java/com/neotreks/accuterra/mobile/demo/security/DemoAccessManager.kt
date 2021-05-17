package com.neotreks.accuterra.mobile.demo.security

import android.content.Context
import androidx.annotation.Keep
import com.neotreks.accuterra.mobile.demo.BuildConfig
import com.neotreks.accuterra.mobile.sdk.IAccessProvider
import com.neotreks.accuterra.mobile.sdk.SdkManager
import com.neotreks.accuterra.mobile.sdk.security.model.ClientCredentials

/**
 * Class for managing access to AccuTerra services
 */
@Keep
class DemoAccessManager : IAccessProvider {

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
