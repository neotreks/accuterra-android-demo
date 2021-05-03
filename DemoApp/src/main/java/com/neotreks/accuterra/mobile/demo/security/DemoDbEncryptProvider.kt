package com.neotreks.accuterra.mobile.demo.security

import android.content.Context
import androidx.preference.PreferenceManager
import com.neotreks.accuterra.mobile.demo.settings.UserSettingsActivity
import com.neotreks.accuterra.mobile.sdk.DBPasscodeBuilder
import com.neotreks.accuterra.mobile.sdk.IDBEncryptConfigProvider

/**
 * The implementation of the [IDBEncryptConfigProvider]
 */
class DemoDbEncryptProvider: IDBEncryptConfigProvider {

    private var temporaryPasscode: ByteArray?

    /**
     * The default constructor must be provided. This is requested by the SDK.
     */
    constructor() {
        this.temporaryPasscode = null
    }

    /**
     * This constructor is intended for `change password` functionality.
     * Do not forget to call the [discardPassword] method after the password is changed!
     */
    constructor(temporaryPasscode: ByteArray) {
        this.temporaryPasscode = temporaryPasscode
    }

    override suspend fun getTripRecordingPasscode(context: Context): ByteArray {
        // Check if the temporary password is present. Provide it if needed.
        val passcode = temporaryPasscode
        if (passcode != null) {
            return passcode
        }
        // Read from the preferences.
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val passcodeString = sharedPreferences.getString(UserSettingsActivity.KEY_TRIP_DB_PASSCODE, null)
        return if (passcodeString == null) {
            byteArrayOf()
        } else {
            DBPasscodeBuilder.buildPasscodeBytes(passcodeString)
        }
    }

    /**
     * Removes the temporary password from the memory.
     */
    fun discardPassword() {
        this.temporaryPasscode = null
    }

}