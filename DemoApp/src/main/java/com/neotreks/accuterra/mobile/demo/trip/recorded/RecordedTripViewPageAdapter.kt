package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.neotreks.accuterra.mobile.demo.R

/**
 * View pager for [RecordedTripActivity]
 */
class RecordedTripViewPageAdapter(
    fm: FragmentManager,
    private val context: Context
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val tabNames = arrayOf(
            R.string.general_map,
            R.string.general_photos,
            R.string.general_pois,
            R.string.general_info,
            R.string.general_statistics
        )

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun getItem(position: Int): Fragment {

        return when (position) {
            0 -> {
                RecordedTripMapFragment()
            }
            1 -> {
                RecordedTripPhotosFragment()
            }
            2 -> {
                RecordedTripPoisFragment()
            }
            3 -> {
                RecordedTripInfoFragment()
            }
            4 -> {
                RecordedTripStatsFragment()
            }
            else -> {
                throw IllegalArgumentException("There is no fragment with position = $position")
            }
        }
    }

    override fun getCount(): Int {
        return tabNames.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return context.getString(tabNames[position])
    }

}