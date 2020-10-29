package com.neotreks.accuterra.mobile.demo.settings

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceManager
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.security.DemoAccessManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserSettingsActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val TAG = "UserSettingsActivity"

        const val KEY_USER_SETTINGS = "user_id"

        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, UserSettingsActivity::class.java)
        }

    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_settings)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.activity_user_settings_container, UserSettingsFragment())
            .commit()

        // Register preference change listener - we need to reset the access token if user is changed
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_USER_SETTINGS) {
                GlobalScope.launch {
                    try {
                        DemoAccessManager().resetToken(this@UserSettingsActivity)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error while resetting access token.", e)
                    }
                }
            }
        }
    }
}