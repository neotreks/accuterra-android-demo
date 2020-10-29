package com.neotreks.accuterra.mobile.demo.trip.community

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.demo.feed.*
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.model.Result
import com.neotreks.accuterra.mobile.sdk.trail.model.MapLocation
import com.neotreks.accuterra.mobile.sdk.ugc.model.*
import kotlinx.coroutines.launch

/**
 * View model for the [CommunityFeedActivity]
 */
class CommunityFeedViewModel: ViewModel() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        val DEFAULT_LOCATION = MapLocation(39.742043, -104.991531) // Denver
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    /**
     * Location used for filtering the activity feed
     */
    var location: MapLocation = DEFAULT_LOCATION

    var listItems = MutableLiveData<List<ActivityFeedItem>>(listOf())

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun hasLocation(): Boolean {
        return location != DEFAULT_LOCATION
    }

    fun loadCommunityTrips(context: Context, criteria: GetCommunityFeedCriteria) {
        viewModelScope.launch {
            val service = ServiceFactory.getTripService(context)
            val result: Result<GetCommunityFeedResult> = service.getCommunityFeed(criteria)
            if (result.isSuccess && result.value != null) {
                val trips = convertToFeedItem((result.value as GetCommunityFeedResult).entries)
                listItems.value = trips
            } else {
                listItems.value = listOf()
                Toast.makeText(context, result.errorMessage ?: "Error", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun reset() {
        listItems.value = listOf()
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun convertToFeedItem(trips: List<ActivityFeedEntry>): List<ActivityFeedItem> {
        val items = mutableListOf<ActivityFeedItem>()
        for(trip in trips) {
            items.add(ActivityFeedTripUserItem(trip))
            items.add(ActivityFeedTripHeaderItem(trip))
            items.add(ActivityFeedTripStatisticsItem(trip))
            items.add(ActivityFeedTripThumbnailItem(trip))
            items.add(ActivityFeedTripUgcFooterItem(trip))
        }
        return items
    }

}