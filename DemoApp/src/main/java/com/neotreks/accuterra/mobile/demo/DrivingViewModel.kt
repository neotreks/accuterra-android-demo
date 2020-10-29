package com.neotreks.accuterra.mobile.demo

import android.content.Context
import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.*
import com.neotreks.accuterra.mobile.sdk.trip.recorder.ITripRecorder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripStatistics
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * View-model for the [DrivingActivity]
 */
class DrivingViewModel: ViewModel() {

    /**
     * Public TRAIL live data
     */
    val trail: MutableLiveData<Trail?> = MutableLiveData(null)
    lateinit var trailDrive: TrailDrive

    val lastLocation: MutableLiveData<Location?> = MutableLiveData(null)

    val navigatorStatus: MutableLiveData<NavigatorStatus> =
        MutableLiveData(NavigatorStatus.createNotReady())

    val sortedWayPoints = mutableListOf<TrailDriveWaypoint>()
    var nextExpectedWayPoint: TrailDriveWaypoint? = null

    /**
     * Loads necessary trail data including waypoints.
     * Sets the selectedPoi value when complete.
     */
    fun loadTrail(trailId: Long, context: Context) {
        viewModelScope.launch {
            val service = ServiceFactory.getTrailService(context)

            val trail = service.getTrailById(trailId)
                ?: throw IllegalArgumentException("Unknown trail ID ${trailId}.")

            // For now always load the first trail drive
            val trailDrive = service.getTrailDrives(trailId).first()

            // keep the order of assignments to trailDrive and trail.
            // trail is an observable and we want to guarantee that trailDrive exists if trail exists
            this@DrivingViewModel.trailDrive = trailDrive
            this@DrivingViewModel.sortedWayPoints.clear()
            this@DrivingViewModel.sortedWayPoints.addAll(trailDrive.waypoints)
            this@DrivingViewModel.trail.value = trail
        }
    }

    /* * * * * * * * * * * * */
    /*       RECORDING       */
    /* * * * * * * * * * * * */

    var tripUUID: String? = null

    val tripStatistics: MutableLiveData<TripStatistics?> = MutableLiveData(null)

    private lateinit var recorder: ITripRecorder

    fun getTripRecorder(context: Context): ITripRecorder {
        if (!::recorder.isInitialized) {
            runBlocking {
                recorder = ServiceFactory.getTripRecorder(context)
                tripUUID = recorder.getActiveTrip()?.tripInfo?.uuid
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

}