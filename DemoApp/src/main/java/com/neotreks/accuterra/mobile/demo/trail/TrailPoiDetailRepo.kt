package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailDriveWaypoint

/**
 * Repository class for the [TrailPoiDetailActivity]
 */
class TrailPoiDetailRepo(private val context: Context) {

    suspend fun getWaypoint(waypointId: Long): TrailDriveWaypoint {
        val service = ServiceFactory.getTrailService(context)
        return service.getTrailDriveWaypoint(waypointId)
    }

}