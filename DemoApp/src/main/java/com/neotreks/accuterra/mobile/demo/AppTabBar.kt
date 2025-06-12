package com.neotreks.accuterra.mobile.demo

import com.google.android.material.tabs.TabLayout

fun TabLayout.setupAppBasicTabs() {
    val discoverTab = this.newTab().apply {
        setText(R.string.discover)
        setIcon(R.drawable.ic_map_24px)
    }
    val communityTab = this.newTab().apply {
        setText(R.string.community)
        setIcon(R.drawable.ic_feed_24px)
    }
    val myTripsTab = this.newTab().apply {
        setText(R.string.my_trips)
        setIcon(R.drawable.ic_bungalow_24px)
    }
    val profileTab = this.newTab().apply {
        setText(R.string.profile)
        setIcon(R.drawable.ic_person_24px)
    }

    if (this.tabCount > 0) {
        this.removeAllTabs()
    }

    this.addTab(discoverTab, AppBasicTabs.TAB_DISCOVER_INDEX)
    this.addTab(communityTab, AppBasicTabs.TAB_COMMUNITY_INDEX)
    this.addTab(myTripsTab, AppBasicTabs.TAB_MY_TRIPS_INDEX)
    this.addTab(profileTab, AppBasicTabs.TAB_PROFILE_INDEX)
}

fun TabLayout.selectTab(index: Int) {
    this.getTabAt(index)?.select()
}