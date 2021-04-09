package com.neotreks.accuterra.mobile.demo.trip.trailcollection

import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.demo.extensions.notifyObserver
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.MapLocationBuilder
import com.neotreks.accuterra.mobile.sdk.trail.model.PointType
import com.neotreks.accuterra.mobile.sdk.trail.model.Tag
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMediaBuilder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi
import com.neotreks.accuterra.mobile.sdk.trip.model.TripStatistics
import com.neotreks.accuterra.mobile.sdk.trip.recorder.ITripRecorder
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * ViewModel for the [TrailCollectionActivity]
 */
class TrailCollectionViewModel() : ViewModel() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    var tripUUID: String? = null

    lateinit var selectedPointType: PointType

    val lastLocation: MutableLiveData<Location?> = MutableLiveData(null)

    val tripStatistics: MutableLiveData<TripStatistics?> = MutableLiveData(null)

    lateinit var tagMapping: MutableMap<Int, Tag>

    var isAddPoiMode = false

    /** Boolean flag indicating unsaved changes */
    var isFormDirty: Boolean = false

    /** The POI which is currently edited */
    val poi: MutableLiveData<TripRecordingPoi?> = MutableLiveData()

    /** Recently taken photo file */
    var cameraFile: File? = null

    /** Location for adding a POI */
    var addPoiLocation: Location? = null

    /** Media of added/edited POI */
    val media = MutableLiveData(mutableListOf<TripRecordingMedia>())

    var poiBackup: PoiBackup? = null

    data class PoiBackup(
        var name: String?,
        var description: String?,
        var pointType: PointType?,
        var tags: List<Tag> = listOf()
    )


    private lateinit var recorder: ITripRecorder

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    suspend fun initTagMapping(context: Context) {
        // Lets create mapping
        val mapping = mutableMapOf<Int, Tag>()
        ServiceFactory.getEnumService(context).getTags().mapIndexed { id, tag ->
            mapping[id] = tag
        }
        this.tagMapping = mapping
    }

    fun getWaypointTags(context: Context, pointType: PointType): List<Tag> {
        return tagMapping.values.filter { it.pointTypeCode == pointType.code }
    }

    fun getTripRecorder(context: Context): ITripRecorder {
        if (!::recorder.isInitialized) {
            runBlocking {
                recorder = ServiceFactory.getTripRecorder(context)
                tripUUID = recorder.getActiveTripRecording()?.tripInfo?.uuid
            }
        }
        return recorder
    }

    /**
     * We use this just to:
     * - update the map position
     * - trigger getting new statistics
     *
     * The location recording is done inside of the [com.neotreks.accuterra.mobile.demo.trip.location.LocationService]
     */
    fun setLastLocation(location: Location?) {
        lastLocation.postValue(location)
        viewModelScope.launch {
            val uuid = tripUUID
            if (uuid != null && location != null) {
                tripStatistics.postValue(recorder.getTripStatistics())
            }
        }
    }

    /**
     * Remember the location when "Add Poi" was clicked
     */
    fun rememberPoiLocation() {
        addPoiLocation = lastLocation.value
    }

    /**
     * Prepare view model for adding new POI
     */
    fun onAddPoiClicked() {
        rememberPoiLocation()
        poi.value = null
        poiBackup = null
        media.value = mutableListOf()
    }

    /**
     * Add the newly created POI to the trip recording
      */
    suspend fun addPoi(poi: TripRecordingPoi, context: Context) {
        val recorder = ServiceFactory.getTripRecorder(context)
        recorder.addPoi(poi)
    }

    /**
     * Updates trip recording POI
     */
    suspend fun updatePoi(poi: TripRecordingPoi, context: Context) {
        val recorder = ServiceFactory.getTripRecorder(context)
        recorder.updatePoi(poi)
    }

    /**
     * Initializes the view model
     */
    suspend fun loadPoi(poiUuid: String, context: Context) {

        val tripService = ServiceFactory.getTripRecordingService(context)
        val loadedPoi = tripService.getTripRecordingPoiByUuid(poiUuid)
            ?: throw IllegalStateException("Cannot find POI by uuid: $poiUuid")

        poiBackup = null
        this.poi.value = loadedPoi

        // Init also the `this.media` for easier manipulation
        this.media.value = loadedPoi.media.toMutableList()
    }

     /**
     * Adds captured photo into the POI
     */
    fun addMedia(file: File, location: Location) {
        if (!file.exists()) {
            throw IllegalStateException("Media file on given path is not available: ${file.path}")
        }
        viewModelScope.launch {
            val mapLocation = MapLocationBuilder.buildFrom(location)
            media.value!!.add(TripRecordingMediaBuilder.buildFromFile(file, location = mapLocation))
            media.notifyObserver()
        }
    }

    /**
     * Adds photo e.g. from the gallery
     */
    fun addMedia(context: Context, uri: Uri, location: Location) {
        viewModelScope.launch {
            val mapLocation = MapLocationBuilder.buildFrom(location)
            media.value!!.add(TripRecordingMediaBuilder.buildFromUri(uri, context, location = mapLocation))
            media.notifyObserver()
        }
    }

    fun deleteMedia(media: TripRecordingMedia) {
        this.media.value!!.remove(media)
        this.media.notifyObserver()
    }

}