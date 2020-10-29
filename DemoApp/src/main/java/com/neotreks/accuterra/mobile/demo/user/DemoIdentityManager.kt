package com.neotreks.accuterra.mobile.demo.user

import android.content.Context
import androidx.preference.PreferenceManager
import com.neotreks.accuterra.mobile.demo.settings.UserSettingsActivity
import com.neotreks.accuterra.mobile.sdk.IIdentityProvider

/**
 * User identity manger
 */
class DemoIdentityManager: IIdentityProvider {

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override suspend fun getUserId(context: Context): String {
        // Read from shared preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        // We need to use default value also here since
        val userId =  sharedPreferences.getString(UserSettingsActivity.KEY_USER_SETTINGS, null)
        return if (userId.isNullOrBlank()) {
            "test driver uuid" // We need to set default value here since settings cannot be reset to null and returns "" instead
        } else {
            userId
        }
    }

}