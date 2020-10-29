package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecording
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi
import kotlinx.coroutines.launch

/**
 * View model for [RecordedTripActivity]
 */
class RecordedTripViewModel : ViewModel() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    lateinit var tripUuid: String

    /**
     * The main trip object
     */
    val trip = MutableLiveData<TripRecording?>()

    val pois = MutableLiveData<List<TripRecordingPoi>>()

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun loadTrip(tripUuid: String, applicationContext: Context) {
        viewModelScope.launch {
            this@RecordedTripViewModel.tripUuid = tripUuid
            val repo = getRepo(applicationContext)
            // Load Trip
            val trip = repo.getTrip(tripUuid)
            this@RecordedTripViewModel.trip.value = trip
            // Load Trip POIs
            if (trip != null) {
                val pois = repo.getTripPois(tripUuid)
                this@RecordedTripViewModel.pois.value = pois
            }
        }
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun getRepo(applicationContext: Context) : RecordedTripRepo {
        return RecordedTripRepo(applicationContext)
    }

}