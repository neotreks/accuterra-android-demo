package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.demo.extensions.notifyObserver
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecording
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMediaUtil
import kotlinx.coroutines.launch
import java.io.File

/**
 * View model for the [TripSaveActivity]
 */
class TripSaveViewModel: ViewModel() {

    var tripRecording: MutableLiveData<TripRecording?> = MutableLiveData(null)

    val media = MutableLiveData(mutableListOf<TripRecordingMedia>())

    fun loadTrip(tripUUID: String, context: Context) {
        viewModelScope.launch {
            val tripService = ServiceFactory.getTripRecordingService(context)
            val loadedTrip = tripService.getTripByUUID(tripUUID)
                ?: throw IllegalStateException("")
            tripRecording.value = loadedTrip
            media.value = loadedTrip.media.toMutableList()
        }
    }

    /**
     * Adds captured photo into the POI
     */
    @MainThread
    fun addMedia(path: String) {
        val file = File(path)
        if (!file.exists()) {
            throw IllegalStateException("Media file on given path is not available: $path")
        }
        viewModelScope.launch {
            media.value!!.add(TripRecordingMediaUtil.buildFromFile(file))
            media.notifyObserver()
        }
    }

    /**
     * Adds photo e.g. from the gallery
     */
    fun addMedia(uri: Uri, context: Context) {
        viewModelScope.launch {
            media.value!!.add(TripRecordingMediaUtil.buildFromUri(uri, context))
            media.notifyObserver()
        }
    }

    fun deleteMedia(media: TripRecordingMedia) {
        this.media.value!!.remove(media)
        this.media.notifyObserver()
    }

    fun canEditTrip(): Boolean {
        return tripRecording.value?.isEditable() ?: false
    }

    fun shuffleMedia() {
        media.value?.shuffle()
        media.notifyObserver()
    }

}