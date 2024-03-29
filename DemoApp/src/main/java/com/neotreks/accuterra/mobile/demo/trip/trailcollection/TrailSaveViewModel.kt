package com.neotreks.accuterra.mobile.demo.trip.trailcollection

import android.content.Context
import android.net.Uri
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.demo.extensions.notifyObserver
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.AccessConcern
import com.neotreks.accuterra.mobile.sdk.trail.model.TechnicalRating
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailTag
import com.neotreks.accuterra.mobile.sdk.trip.model.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * View model for the [TrailSaveActivity]
 */
class TrailSaveViewModel: ViewModel() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    var tripRecording: MutableLiveData<TripRecording?> = MutableLiveData(null)
    lateinit var tripPois: List<TripRecordingPoi>

    val tripMedia = MutableLiveData(mutableListOf<TripRecordingMedia>())
    var poiMedia = MutableLiveData<List<TripRecordingMedia>>()
    var preferredImageUuid: String? = null

    var techRatingsHigh: List<TechnicalRating> = listOf()
    var techRatingsLow: List<TechnicalRating> = listOf()
    var accessConcerns: List<AccessConcern> = listOf()

    lateinit var trailTagMapping: MutableMap<Int, TrailTag>

    var isFormDirty: Boolean = false

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun loadTrip(tripUUID: String, context: Context) {
        viewModelScope.launch {
            loadEnums(context)
            val tripService = ServiceFactory.getTripRecordingService(context)
            val loadedTrip = tripService.getTripRecordingByUUID(tripUUID)
                ?: throw IllegalStateException("")
            tripPois = tripService.getTripRecordingPOIs(tripUUID)
            tripRecording.value = loadedTrip
            // Media attached to Trip
            tripMedia.value = loadedTrip.media.toMutableList()
            // Media attached to Trip Pois
            // List trail and point media URls, use thumbnails if possible
            val pois = tripService.getTripRecordingPOIs(tripUUID)
            val poiMedia = pois.flatMap { it.media }
            this@TrailSaveViewModel.poiMedia.value = poiMedia
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
            tripMedia.value!!.add(TripRecordingMediaBuilder.buildFromFile(file))
            tripMedia.notifyObserver()
        }
    }

    /**
     * Adds photo e.g. from the gallery
     */
    fun addMedia(uri: Uri, context: Context) {
        viewModelScope.launch {
            tripMedia.value!!.add(TripRecordingMediaBuilder.buildFromUri(context, uri))
            tripMedia.notifyObserver()
        }
    }

    fun deleteMedia(media: TripRecordingMedia) {
        this.tripMedia.value!!.remove(media)
        this.tripMedia.notifyObserver()
    }

    fun canEditTrip(): Boolean {
        return tripRecording.value?.isEditable() ?: false
    }

    fun validateHighLowRating(technicalRatingHigh: Int?, technicalRatingLow:Int?):Boolean {
        return technicalRatingHigh!! >= technicalRatingLow!!
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private suspend fun loadEnums(context: Context) {
        techRatingsHigh = ServiceFactory.getEnumService(context).getTechRatings()
        techRatingsLow = ServiceFactory.getEnumService(context).getTechRatings()
        accessConcerns = ServiceFactory.getEnumService(context).getAccessConcerns()
        trailTagMapping = tagMapping(context)
    }

    private suspend fun tagMapping(context: Context): MutableMap<Int, TrailTag> {
        val mapping = mutableMapOf<Int, TrailTag>()
        val tags = ServiceFactory.getEnumService(context).getTrailTags()
        tags.forEachIndexed { id, tag ->
            mapping[id] = tag
        }
        return mapping
    }

}