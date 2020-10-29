package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi

/**
 * Repository for the [RecordedTripPoiViewModel]
 */
class RecordedTripPoiRepo(private val context: Context) {

    suspend fun loadTripPoi(poiUuid: String) : TripRecordingPoi {

        val tripService = ServiceFactory.getTripRecordingService(context)
        return tripService.getTripPoiByUuid(poiUuid)
            ?: throw IllegalStateException("Cannot find POI by uuid: $poiUuid")

    }

}