package com.neotreks.accuterra.mobile.demo.upload

import android.content.Context
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.sync.model.UploadRequest

/**
 * Repository for the [ObjectUploadViewModel]
 */
class ObjectUploadRepo(context: Context) {

    private val service = ServiceFactory.getSynchronizationService(context)

    suspend fun getUploadRequests(objectUUID: String, versionUuid: String?): List<UploadRequest> {
        return service.getUploadRequestsForObject(objectUUID, versionUuid)
    }

}