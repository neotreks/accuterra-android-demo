package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.demo.extensions.notifyObserver
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.MapLocationBuilder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecording
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMediaBuilder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi
import kotlinx.coroutines.launch
import java.io.File

/**
 * View model for the [TripAddPoiActivity]
 */
class TripAddPoiViewModel: ViewModel() {

    var useTripRecorder: Boolean = true
    val location = MutableLiveData<Location>(null)
    val trip = MutableLiveData<TripRecording>(null)
    val media = MutableLiveData(mutableListOf<TripRecordingMedia>())

    /**
     * Initializes the view model
     */
    suspend fun loadTrip(context: Context, tripUuid: String, location: Location) {

        if (useTripRecorder) {

            // Trip Recording is active - we want to use trip recording.
            val tripService = ServiceFactory.getTripRecorder(context)
            val trip = tripService.getActiveTripRecording()
                ?: throw IllegalStateException("There is no currently recorded trip!")

            this.location.value = location
            this.trip.value = trip

        } else {
            // We are in case of trip editing - we want to use the `Trip Recording Service`
            val tripService = ServiceFactory.getTripRecordingService(context)
            val trip = tripService.getTripRecordingByUUID(tripUuid)
                ?: throw IllegalStateException("There is no trip of uuid: $tripUuid")

            if (!trip.isEditable()) {
                throw IllegalStateException("The trip recording $tripUuid is not editable!")
            }

            this.location.value = location
            this.trip.value = trip
        }

    }

    /**
     * Add the newly created POI to the trip recording
      */
    suspend fun addPoi(context: Context, poi: TripRecordingPoi) {
        if (useTripRecorder) {
            val recorder = ServiceFactory.getTripRecorder(context)
            recorder.addPoi(poi)
        } else {
            val service = ServiceFactory.getTripRecordingService(context)
            val tripUuid = trip.value?.tripInfo?.uuid ?: throw IllegalStateException("Missing tripUuid")
            service.addTripRecordingPoi(tripUuid, poi)
        }
    }

    /**
     * Adds captured photo into the POI
     */
    fun addMedia(file: File) {
        if (!file.exists()) {
            throw IllegalStateException("Media file on given path is not available: ${file.path}")
        }
        viewModelScope.launch {
            val mapLocation = MapLocationBuilder.buildFrom(location.value!!)
            media.value!!.add(TripRecordingMediaBuilder.buildFromFile(file, location = mapLocation))
            media.notifyObserver()
        }
    }

    /**
     * Adds photo e.g. from the gallery
     */
    fun addMedia(uri: Uri, context: Context) {
        viewModelScope.launch {
            val mapLocation = MapLocationBuilder.buildFrom(location.value!!)
            media.value!!.add(TripRecordingMediaBuilder.buildFromUri(context, uri, location = mapLocation))
            media.notifyObserver()
        }
    }

    fun deleteMedia(media: TripRecordingMedia) {
        this.media.value!!.remove(media)
        this.media.notifyObserver()
    }

}