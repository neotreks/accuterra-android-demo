package com.neotreks.accuterra.mobile.demo.settings

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.preference.PreferenceManager
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityUserSettingsBinding
import com.neotreks.accuterra.mobile.demo.security.DemoCredentialsAccessManager
import com.neotreks.accuterra.mobile.demo.util.CrashSupport
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserSettingsActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: ActivityUserSettingsBinding

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val TAG = "UserSettingsActivity"

        const val KEY_USER_SETTINGS = "user_id"
        const val KEY_LOCATION_PROVIDER = "trip_location_provider"
        const val KEY_TRIP_DB_PASSCODE = "db_trip_db_passcode"

        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, UserSettingsActivity::class.java)
        }

    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the default fragment
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
                        DemoCredentialsAccessManager().resetToken(this@UserSettingsActivity)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error while resetting access token.", e)
                        CrashSupport.reportError(e, "Error while resetting access token")
                    }
                }
            }
        }

        // Build the toolbar
        setupToolbar()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun setupToolbar() {

        setSupportActionBar(binding.activityUserSettingsToolbar.generalToolbar)
        binding.activityUserSettingsToolbar.generalToolbarTitle.text = getString(R.string.general_user_settings)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

    }
}