package com.neotreks.accuterra.mobile.demo

import android.app.Activity
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.model.Result
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailUserData
import com.neotreks.accuterra.mobile.sdk.ugc.model.GetTrailCommentsResult
import com.neotreks.accuterra.mobile.sdk.ugc.service.GetTrailCommentsCriteria

/**
 * Repo for the [TrailInfoViewModel]
 */
class TrailInfoRepo {

    suspend fun loadTrailComments(context: Activity, trailId: Long): Result<GetTrailCommentsResult?> {
        val service = ServiceFactory.getTrailService(context)
        val criteria = GetTrailCommentsCriteria.build(trailId)
        return service.getTrailComments(criteria)
    }

    suspend fun loadTrailUserData(context: Activity, trailId: Long): Result<TrailUserData?> {
        val service = ServiceFactory.getTrailService(context)
        return service.getTrailUserData(trailId)
    }

}