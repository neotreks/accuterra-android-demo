package com.neotreks.accuterra.mobile.demo.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import kotlinx.coroutines.launch

/**
 * View Model for the [MediaDetailActivity]
 */
class MediaDetailViewModel: ViewModel() {

    val uri = MutableLiveData<Uri>()

    fun loadMediaFromUuid(uuid: String, context: Context) {
        viewModelScope.launch {
            val service = ServiceFactory.getTripMediaService(context)
            val media = service.getTripRecordingMediaByUuid(uuid)
                ?: throw IllegalStateException("Cannot find trip media for :$uuid")
            uri.value = media.uri
        }
    }

    fun loadMediaFromUrl(url: String, context: Context) {
        viewModelScope.launch {
            // Trail media are defined by URL and are loaded using the ITrailMediaService
            val service = ServiceFactory.getTrailMediaService(context)
            val localUri = service.getMediaFile(url)
                ?: throw IllegalStateException("Cannot find trail media for :$url")
            uri.value = localUri
        }
    }

    fun loadMediaFromUri(uri: Uri) {
        this.uri.value = uri
    }

}