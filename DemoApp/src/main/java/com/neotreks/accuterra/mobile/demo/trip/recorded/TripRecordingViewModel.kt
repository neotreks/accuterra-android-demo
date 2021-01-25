package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trip.recorder.ITripRecorder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripStatistics
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * ViewModel for the [TripRecordingActivity]
 */
class TripRecordingViewModel: ViewModel() {

    var tripUUID: String? = null

    val lastLocation: MutableLiveData<Location?> = MutableLiveData(null)

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