package com.neotreks.accuterra.mobile.demo.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.*
import com.neotreks.accuterra.mobile.demo.databinding.ActivityProfileBinding
import com.neotreks.accuterra.mobile.demo.extensions.applyAllWindowInsetsButStatusBar
import com.neotreks.accuterra.mobile.demo.extensions.applyStatusBarWindowInsets
import com.neotreks.accuterra.mobile.demo.offlinemap.OfflineMapsActivity
import com.neotreks.accuterra.mobile.demo.security.DemoCredentialsAccessManager
import com.neotreks.accuterra.mobile.demo.settings.UserSettingsActivity
import com.neotreks.accuterra.mobile.demo.ui.ProgressDialogHolder
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.sdk.SdkManager
import com.neotreks.accuterra.mobile.sdk.trail.model.MapLocation

class ProfileActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "ProfileActivity"
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
        applyAllWindowInsetsButStatusBar(binding.root)
        applyStatusBarWindowInsets(binding.newTripActivityToolbar.root)
        setupListeners()
        setupTabListener()
        UiUtils.setApkVersionText(binding.newTripActivityToolbar.generalToolbarSdkVersion)
    }

    override fun onResume() {
        super.onResume()
        binding.activityProfileTabs.componentBasicTabs.selectTab(AppBasicTabs.TAB_PROFILE_INDEX)
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
        binding.activityProfileDeleteUserDataButton.setOnClickListener {
            onDeleteUserData()
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
                DemoCredentialsAccessManager().resetToken(this@ProfileActivity)
                this@ProfileActivity.toast(getString(R.string.upload_reset_access_token_done))
            } catch (e: Exception) {
                this@ProfileActivity.toast(getString(R.string.upload_reset_access_token_failed, e.localizedMessage))
            } finally {
                dialogHolder.hideProgressDialog()
            }
        }
    }

    private fun onDeleteUserData() {
        // Provide question dialog for user data deletion
        DialogUtil.buildYesNoDialog(
            context = this,
            title = getString(R.string.activity_profile_delete_user_data_dialog_title),
            message = getString(R.string.activity_profile_delete_user_data_dialog_message),
            negativeCodeLabel = getString(R.string.general_yes_delete),
            negativeCode = {
                deleteUserData()
            },
            positiveCodeLabel = getString(R.string.general_cancel)
        ).show()
    }

    private fun deleteUserData() {
        dialogHolder.displayProgressDialog(
            this,
            getString(R.string.activity_profile_delete_user_data_progress)
        )
        lifecycleScope.launchWhenCreated {
            try {
                SdkManager.deleteUserData(this@ProfileActivity)
                this@ProfileActivity.toast(getString(R.string.activity_profile_delete_user_data_success))
            } catch (e: Exception) {
                Log.e(TAG, "User dat not deleted because of: ${e.localizedMessage}", e)
                this@ProfileActivity.toast(
                    getString(
                        R.string.activity_profile_delete_user_data_failure,
                        e.localizedMessage
                    )
                )
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
        binding.activityProfileTabs.componentBasicTabs.setupAppBasicTabs()
        binding.activityProfileTabs.componentBasicTabs.selectTab(AppBasicTabs.TAB_PROFILE_INDEX)
        binding.activityProfileTabs.componentBasicTabs.addOnTabSelectedListener(
            MainTabListener(object : MainTabListener.MainTabListenerHelper {
                override val context: Activity
                    get() = this@ProfileActivity
                override fun getCurrentTabPosition(): Int {
                    return AppBasicTabs.TAB_PROFILE_INDEX
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

}