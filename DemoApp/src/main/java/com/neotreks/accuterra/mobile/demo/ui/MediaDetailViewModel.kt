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

    val uri = MutableLiveData<Uri?>()

    fun loadMediaFromUuid(uuid: String, context: Context) {
        viewModelScope.launch {
            val service = ServiceFactory.getTripMediaService(context)
            val media = service.getTripRecordingMediaByUuid(uuid)
                ?: throw IllegalStateException("Cannot find trip media for :$uuid")
            if (media.isLocalMedia) {
                // This is the case of standard new `Trip Recording`
                // Media are available locally and we can display these via `TripRecordingMedia.uri`
                uri.value = media.uri
            } else {
                // This is a case of the `Online Trip Editing`.
                // Media files are not stored locally but are available on the server.
                // So we have to display the media as we do for `Online Trip Media`
                // The base URL is available in the `TripRecordingMedia.serverMediaUrl`
                media.serverMedialUrl?.let { serverUrl ->
                    loadMediaFromUrl(serverUrl, context)
                }
            }
        }
    }

    fun loadMediaFromUrl(url: String, context: Context) {
        viewModelScope.launch {
            // Trail media are defined by URL and are loaded using the ITrailMediaService
            val service = ServiceFactory.getTrailMediaService(context)
            val localUri = service.getMediaFile(url)
            uri.value = localUri
        }
    }

    fun loadMediaFromUri(uri: Uri) {
        this.uri.value = uri
    }

}