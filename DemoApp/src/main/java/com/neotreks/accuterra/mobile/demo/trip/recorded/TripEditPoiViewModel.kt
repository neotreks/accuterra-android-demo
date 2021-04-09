package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.demo.extensions.notifyObserver
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecording
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMediaBuilder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi
import kotlinx.coroutines.launch
import java.io.File

/**
 * View model for the [TripEditPoiActivity]
 */
class TripEditPoiViewModel: ViewModel() {

    var poi = MutableLiveData<TripRecordingPoi>()
    // Keeping media separately from TripPoi for easier manipulation
    val media = MutableLiveData(mutableListOf<TripRecordingMedia>())
    var tripRecording: TripRecording? = null

    /**
     * Initializes the view model
     */
    suspend fun loadPoi(poiUuid: String, context: Context) {

        val tripService = ServiceFactory.getTripRecordingService(context)
        val loadedPoi = tripService.getTripRecordingPoiByUuid(poiUuid)
            ?: throw IllegalStateException("Cannot find POI by uuid: $poiUuid")

        this.poi.value = loadedPoi

        // Init also the `this.media` for easier manipulation
        this.media.value = loadedPoi.media.toMutableList()
        // Load trip to get it's status
        tripRecording = tripService.getTripRecordingByPoiUuid(poiUuid)
    }

    /**
     * Adds captured photo into the POI
     */
    fun addMedia(file: File) {
        if (!file.exists()) {
            throw IllegalStateException("Media file on given path is not available: ${file.path}")
        }
        viewModelScope.launch {
            val mapLocation = poi.value!!.mapLocation
            media.value!!.add(TripRecordingMediaBuilder.buildFromFile(file, location = mapLocation))
            media.notifyObserver()
        }
    }

    /**
    * Adds photo e.g. from the gallery
    */
    fun addMedia(uri: Uri, context: Context) {
        viewModelScope.launch {
            val mapLocation = poi.value!!.mapLocation
            media.value!!.add(TripRecordingMediaBuilder.buildFromUri(uri, context, location = mapLocation))
            media.notifyObserver()
        }
    }

    fun deleteMedia(media: TripRecordingMedia) {
        this.media.value!!.remove(media)
        this.media.notifyObserver()
    }

    fun canDeletePoi(): Boolean {
        return tripRecording?.isEditable() ?: false
    }

    fun shuffleMedia() {
        media.value?.shuffle()
        media.notifyObserver()
    }

}