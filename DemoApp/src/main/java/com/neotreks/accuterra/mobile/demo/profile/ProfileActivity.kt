package com.neotreks.accuterra.mobile.demo.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.*
import com.neotreks.accuterra.mobile.demo.databinding.ActivityProfileBinding
import com.neotreks.accuterra.mobile.demo.offlinemap.OfflineMapsActivity
import com.neotreks.accuterra.mobile.demo.security.DemoAccessManager
import com.neotreks.accuterra.mobile.demo.settings.UserSettingsActivity
import com.neotreks.accuterra.mobile.demo.ui.ProgressDialogHolder
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.sdk.trail.model.MapLocation

class ProfileActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, ProfileActivity::class.java)
        }
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private var dialogHolder = ProgressDialogHolder()

    private lateinit var binding: ActivityProfileBinding

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
        setupTabListener()
        selectCurrentTab()
        UiUtils.setApkVersionText(binding.newTripActivityToolbar.generalToolbarSdkVersion)
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun setupListeners() {

        // Settings
        binding.activityProfileSettingsButton.setOnClickListener {
            val intent = UserSettingsActivity.createNavigateToIntent(this)
            startActivity(intent)
        }
        // Reset access Token
        binding.activityProfileResetTokenButton.setOnClickListener {
            resetAccessToken()
        }
        // Trip Recording Database Passcode
        binding.activityProfileDbPasscodeButton.setOnClickListener {
            editTripRecordingDbPasscode()
        }
        // Download Offline Maps
        binding.activityProfileDownloadOfflineMapsButton.setOnClickListener {
            downloadOfflineMaps()
        }
    }

    private fun resetAccessToken() {
        dialogHolder.displayProgressDialog(this, getString(R.string.upload_reset_access_token))
        lifecycleScope.launchWhenCreated {
            try {
                DemoAccessManager().resetToken(this@ProfileActivity)
                this@ProfileActivity.toast(getString(R.string.upload_reset_access_token_done))
            } catch (e: Exception) {
                this@ProfileActivity.toast(getString(R.string.upload_reset_access_token_failed, e.localizedMessage))
            } finally {
                dialogHolder.hideProgressDialog()
            }
        }
    }

    private fun downloadOfflineMaps() {
        lifecycleScope.launchWhenCreated {
            val intent = OfflineMapsActivity.createNavigateToIntent(this@ProfileActivity)
            startActivity(intent)
            finish()
        }
    }

    private fun editTripRecordingDbPasscode() {
        lifecycleScope.launchWhenCreated {
            val intent = DbPasscodeActivity.createNavigateToIntent(this@ProfileActivity)
            startActivity(intent)
        }
    }

    private fun setupTabListener() {
        binding.activityProfileTabs.componentBasicTabs.addOnTabSelectedListener(
            MainTabListener(object : MainTabListener.MainTabListenerHelper {
                override val context: Activity
                    get() = this@ProfileActivity
                override fun getCurrentTabPosition(): Int {
                    return this@ProfileActivity.getCurrentTabPosition()
                }
                override fun shouldFinish(): Boolean {
                    return true
                }
                override fun getCommunityFeedLocation(): MapLocation? {
                    return null
                }
            })
        )
    }

    private fun getCurrentTabPosition(): Int {
        return AppBasicTabs.TAB_PROFILE_INDEX
    }

    private fun selectCurrentTab() {
        binding.activityProfileTabs.componentBasicTabs.getTabAt(getCurrentTabPosition())!!.select()
    }

}