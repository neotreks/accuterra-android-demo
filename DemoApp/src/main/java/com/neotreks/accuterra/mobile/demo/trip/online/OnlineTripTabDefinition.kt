package com.neotreks.accuterra.mobile.demo.trip.online

import androidx.fragment.app.Fragment
import com.neotreks.accuterra.mobile.demo.R

/**
 * Definitions of tabs on the [OnlineTripActivity]
 */
enum class OnlineTripTabDefinition(
    val position: Int,
    val nameResourceId: Int
) {

    MAP(0,  R.string.general_map),
    PHOTO(1,  R.string.general_photos),
    INFO(2,  R.string.general_info),
    POIS(3,  R.string.general_pois),
    STAT(4, R.string.general_statistics);

    fun getFragment() : Fragment {
        return getFragmentForPosition(position)
    }

    companion object {

        private val tabs = listOf(
            MAP, PHOTO, INFO, POIS, STAT
        )

        private val tabNames = arrayOf(
            R.string.general_map,
            R.string.general_photos,
            R.string.general_info,
            R.string.general_pois,
            R.string.general_statistics
        )

        fun getTabsCount(): Int {
            return tabs.size
        }

        fun getFragmentForPosition(position: Int): Fragment {

            return when (position) {
                0 -> {
                    OnlineTripMapFragment()
                }
                1 -> {
                    OnlineTripPhotosFragment()
                }
                2 -> {
                    OnlineTripInfoFragment()
                }
                3 -> {
                    OnlineTripPoisFragment()
                }
                4 -> {
                    OnlineTripStatsFragment()
                }
                else -> {
                    throw IllegalArgumentException("There is no fragment with position = $position")
                }
            }
        }

        fun getTabName(position: Int): Int {
            return tabs.find { it.position == position }?.nameResourceId
                ?: throw IllegalStateException("No tab defined for position: $position")
        }

    }

}