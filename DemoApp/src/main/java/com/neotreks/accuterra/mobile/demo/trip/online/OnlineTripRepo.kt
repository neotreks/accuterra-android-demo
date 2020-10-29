package com.neotreks.accuterra.mobile.demo.trip.online

import android.app.Activity
import android.content.Context
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trip.model.Trip
import com.neotreks.accuterra.mobile.sdk.model.Result
import com.neotreks.accuterra.mobile.sdk.ugc.model.GetTripCommentsResult
import com.neotreks.accuterra.mobile.sdk.ugc.service.GetTripCommentsCriteriaBuilder

/**
 * Repository class for the [OnlineTripViewModel]
 */
class OnlineTripRepo {

    suspend fun loadTrip(context: Context, uuid: String): Result<Trip?> {
        val service = ServiceFactory.getTripService(context)
        return service.getTrip(uuid)
    }

    suspend fun loadTripComments(context: Activity, tripUuid: String): Result<GetTripCommentsResult> {
        val service = ServiceFactory.getTripService(context)
        val criteria = GetTripCommentsCriteriaBuilder.build(tripUuid)
        return service.getTripComments(criteria)
    }

}