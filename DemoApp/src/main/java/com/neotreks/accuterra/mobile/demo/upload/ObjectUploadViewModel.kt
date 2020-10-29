package com.neotreks.accuterra.mobile.demo.upload

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neotreks.accuterra.mobile.sdk.sync.model.UploadRequest

/**
 * View model for the [ObjectUploadActivity]
 */
class ObjectUploadViewModel : ViewModel() {

    val uuid: MutableLiveData<String?> = MutableLiveData(null)
    val requests: MutableLiveData<List<UploadRequest>> = MutableLiveData()

    suspend fun loadUploadRequests(context: Context, objectUUID: String) {
        // Set the UUID
        uuid.postValue(objectUUID)
        // Load the list
        val repo = ObjectUploadRepo(context)
        requests.postValue(repo.getUploadRequests(objectUUID))
    }

    suspend fun refresh(context: Context) {
        uuid.value?.let { uuid ->
            loadUploadRequests(context, uuid)
        }
    }

}