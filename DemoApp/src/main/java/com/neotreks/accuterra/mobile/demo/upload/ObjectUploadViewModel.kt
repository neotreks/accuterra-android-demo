package com.neotreks.accuterra.mobile.demo.upload

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.sync.model.UploadRequest
import com.neotreks.accuterra.mobile.sdk.sync.model.UploadWorkerInfo

/**
 * View model for the [ObjectUploadActivity]
 */
class ObjectUploadViewModel : ViewModel() {

    val uuid: MutableLiveData<String?> = MutableLiveData(null)
    val versionUuid: MutableLiveData<String?> = MutableLiveData(null)
    val requests: MutableLiveData<List<UploadRequest>> = MutableLiveData()
    var workerInfo: MutableLiveData<List<UploadWorkerInfo>> = MutableLiveData()

    suspend fun loadData(context: Context, objectUUID: String, versionUuid: String?) {
        // Set the UUID
        this.uuid.postValue(objectUUID)
        this.versionUuid.postValue(versionUuid)
        // Load the list
        val repo = ObjectUploadRepo(context)
        requests.postValue(repo.getUploadRequests(objectUUID, versionUuid))
        // Refresh worker info
        val service = ServiceFactory.getSynchronizationService(context)
        workerInfo.value = service.getUploadWorkerInfo()
    }

    suspend fun refresh(context: Context) {
        // Refresh list of upload requests
        uuid.value?.let { uuid ->
            versionUuid.value?.let { versionUuid ->
                loadData(context, uuid, versionUuid)
            }
        }
    }

}