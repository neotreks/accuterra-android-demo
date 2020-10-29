package com.neotreks.accuterra.mobile.demo.profile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.*
import com.neotreks.accuterra.mobile.demo.security.DemoAccessManager
import com.neotreks.accuterra.mobile.demo.settings.UserSettingsActivity
import com.neotreks.accuterra.mobile.demo.ui.ProgressDialogHolder
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.sdk.trail.model.MapLocation
import kotlinx.android.synthetic.main.accuterra_toolbar.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.component_basic_tabs.*

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

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        setupListeners()
        setupTabListener()
        selectCurrentTab()
        UiUtils.setApkVersionText(general_toolbar_sdk_version)
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun setupListeners() {
        // Settings
        activity_profile_settings_button.setOnClickListener {
            val intent = UserSettingsActivity.createNavigateToIntent(this)
            startActivity(intent)
        }
        // Reset access Token
        activity_profile_reset_token_button.setOnClickListener {
            resetAccessToken()
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

    private fun setupTabListener() {
        component_basic_tabs.addOnTabSelectedListener(
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
        component_basic_tabs.getTabAt(getCurrentTabPosition())!!.select()
    }

}