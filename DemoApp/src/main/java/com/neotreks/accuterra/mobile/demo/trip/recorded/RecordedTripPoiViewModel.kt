package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecording
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi

/**
 * View model for the [TripAddPoiActivity]
 */
class RecordedTripPoiViewModel: ViewModel() {

    var poi = MutableLiveData<TripRecordingPoi>()
    // Keeping media separately from TripPoi for easier manipulation
    val media = MutableLiveData(mutableListOf<TripRecordingMedia>())
    var tripRecording: TripRecording? = null

    /**
     * Initializes the view model
     */
    suspend fun loadPoi(poiUuid: String, context: Context) {

        val repo = RecordedTripPoiRepo(context)
        val loadedPoi = repo.loadTripPoi(poiUuid)

        // Set the POI
        this.poi.value = loadedPoi

        // Init also the `this.media` for easier manipulation
        this.media.value = loadedPoi.media.toMutableList()

    }

}