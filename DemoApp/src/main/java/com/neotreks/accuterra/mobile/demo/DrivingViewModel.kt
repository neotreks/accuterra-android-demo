package com.neotreks.accuterra.mobile.demo

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.Trail
import kotlinx.coroutines.launch

/**
 * View-model for the [DrivingActivity]
 */
class DrivingViewModel: ViewModel() {

    var trailId: Long = Long.MIN_VALUE
        private set

    var selectedPoiId: Long? = null

    /**
     * Public TRAIL live data
     */
    var trail: MutableLiveData<Trail?> = MutableLiveData(null)

    fun setTrailId(trailId: Long, context: Context) {
        require(trailId != Long.MIN_VALUE) { "Invalid trail ID value." }
        this.trailId = trailId
        loadTrail(context)
    }

    /**
     * Loads necessary trail data including waypoints.
     */
    private fun loadTrail(context: Context) {
        viewModelScope.launch {
            val trailObject = ServiceFactory.getTrailService(context).getTrailById(trailId)
                ?: throw IllegalArgumentException("Unknown trail ID ${trailId}.")
            trail.value = trailObject
        }
    }

}