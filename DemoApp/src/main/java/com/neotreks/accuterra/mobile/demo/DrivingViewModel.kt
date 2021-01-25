package com.neotreks.accuterra.mobile.demo

import android.app.Activity
import android.content.Context
import android.location.Location
import android.util.Log
import android.widget.Toast
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

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "DrivingViewModel"
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

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
            try {
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
            } catch (e: Exception) {
                Log.e(TAG, "Error while loading trail: ${e.localizedMessage}", e)
                Toast.makeText(context, context.getString(R.string.trail_errors_load_trail_failed_because_of,
                    e.localizedMessage), Toast.LENGTH_LONG).show()
                // Exit the activity - we cannot continue and use it
                (context as Activity).finish()
            }
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

}