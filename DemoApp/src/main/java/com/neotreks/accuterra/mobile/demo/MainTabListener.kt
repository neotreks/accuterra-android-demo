package com.neotreks.accuterra.mobile.demo

import android.app.Activity
import android.util.Log
import com.google.android.material.tabs.TabLayout
import com.neotreks.accuterra.mobile.demo.profile.ProfileActivity
import com.neotreks.accuterra.mobile.demo.trip.MyTripsActivity
import com.neotreks.accuterra.mobile.demo.trip.community.CommunityFeedActivity
import com.neotreks.accuterra.mobile.sdk.trail.model.MapLocation

/**
 * A default implementation of the main tab listener
 */
class MainTabListener(private val helper: MainTabListenerHelper) : TabLayout.OnTabSelectedListener {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "MainTabListener"
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onTabSelected(tab: TabLayout.Tab?) {
        if (tab == null) {
            return
        }
        when (tab.position) {
            helper.getCurrentTabPosition() -> {
                // Nothing to do here
                Log.d(TAG, "Same tab position")
            }
            AppBasicTabs.TAB_COMMUNITY_INDEX -> {
                val location = helper.getCommunityFeedLocation()
                val intent = CommunityFeedActivity.createNavigateToIntent(helper.context, location)
                helper.context.startActivity(intent)
                finishIfNeeded(helper)
            }
            AppBasicTabs.TAB_MY_TRIPS_INDEX -> {
                val intent = MyTripsActivity.createNavigateToIntent(helper.context)
                helper.context.startActivity(intent)
                finishIfNeeded(helper)
            }
            AppBasicTabs.TAB_DISCOVER_INDEX -> {
                // Just finish this activity - fallback to discovery activity
                finishIfNeeded(helper)
            }
            AppBasicTabs.TAB_PROFILE_INDEX -> {
                val intent = ProfileActivity.createNavigateToIntent(helper.context)
                helper.context.startActivity(intent)
                finishIfNeeded(helper)
            }
            else -> {
                if (tab.position != getDefaultTabIndex()) {
                    helper.context.toast("TODO: Transition to another screen")
                    finishIfNeeded(helper)
                }
            }
        }
    }

    private fun finishIfNeeded(helper: MainTabListenerHelper) {
        if (helper.shouldFinish()) {
            helper.context.finish()
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {}

    override fun onTabReselected(tab: TabLayout.Tab?) {}

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun getDefaultTabIndex(): Any? {
        return AppBasicTabs.TAB_DISCOVER_INDEX
    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    interface MainTabListenerHelper {
        val context: Activity
        fun getCurrentTabPosition(): Int
        /**
         * Indicate if should be finished when leaving
         */
        fun shouldFinish(): Boolean

        /**
         * Provides location for the `community feed` tab if possible
         */
        fun getCommunityFeedLocation(): MapLocation?
    }

}