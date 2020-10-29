package com.neotreks.accuterra.mobile.demo.trip

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.demo.feed.*
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.model.Result
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingBasicInfo
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingSearchCriteria
import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry
import com.neotreks.accuterra.mobile.sdk.ugc.model.GetMyActivityFeedCriteria
import com.neotreks.accuterra.mobile.sdk.ugc.model.GetMyActivityFeedResult
import kotlinx.coroutines.launch

/**
 * View model for the [MyTripsActivity]
 */
class MyTripsViewModel: ViewModel() {

    companion object {
        private const val TAG = "MyTripsViewModel"
    }

    var listItems = MutableLiveData<List<ActivityFeedItem>>(listOf())

    fun loadRecordedTrips(context: Context, criteria: TripRecordingSearchCriteria) {
        viewModelScope.launch {
            val service = ServiceFactory.getTripRecordingService(context)
            val trips = service.findTrips(criteria)
            listItems.value = convertRecordedTripToFeedItem(trips)
        }
    }

    fun loadOnlineTrips(context: Activity, criteria: GetMyActivityFeedCriteria) {
        viewModelScope.launch {
            try {
                val service = ServiceFactory.getTripService(context)
                val result: Result<GetMyActivityFeedResult> = service.getMyActivityFeed(criteria)
                if (result.isSuccess && result.value != null) {
                    val trips = convertToFeedItem((result.value as GetMyActivityFeedResult).entries)
                    listItems.value = trips
                } else {
                    listItems.value = listOf()
                    Toast.makeText(context, result.errorMessage ?: "Error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error wile loading online trips", e)
                context.longToast("Error while loading online trips: ${e.localizedMessage}")
            }
        }
    }

    fun reset() {
        listItems.value = listOf()
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun convertRecordedTripToFeedItem(tripRecordings: List<TripRecordingBasicInfo>): List<ActivityFeedItem> {
        val items = mutableListOf<ActivityFeedItem>()
        for(trip in tripRecordings) {
            items.add(
                ActivityFeedRecordedTripItem(trip)
            )
        }
        return items
    }

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