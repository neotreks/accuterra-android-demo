package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecording
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi

/**
 * Repository class for [RecordedTripActivity]
 */
class RecordedTripRepo(applicationContext: Context) {

    private val tripService = ServiceFactory.getTripRecordingService(applicationContext)

    suspend fun getTrip(tripUuid: String): TripRecording? {
        return tripService.getTripByUUID(tripUuid)
    }

    suspend fun getTripPois(tripUuid: String): List<TripRecordingPoi> {
        return tripService.getTripPOIs(tripUuid)
    }

}