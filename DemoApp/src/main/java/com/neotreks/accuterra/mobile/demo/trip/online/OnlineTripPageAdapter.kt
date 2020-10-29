package com.neotreks.accuterra.mobile.demo.trip.online

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

/**
 * View pager for [OnlineTripActivity]
 */
class OnlineTripPageAdapter(
    fm: FragmentManager,
    private val context: Context
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun getItem(position: Int): Fragment {
        return OnlineTripTabDefinition.getFragmentForPosition(position)
    }

    override fun getCount(): Int {
        return OnlineTripTabDefinition.getTabsCount()
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.getString(OnlineTripTabDefinition.getTabName(position))
    }

}