package com.neotreks.accuterra.mobile.demo

import android.content.Context
import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.map.TrailNavigator
import com.neotreks.accuterra.mobile.sdk.map.TrailNavigatorSituation
import com.neotreks.accuterra.mobile.sdk.trail.model.MapPoint
import com.neotreks.accuterra.mobile.sdk.trail.model.Trail
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailPath
import kotlinx.coroutines.launch

/**
 * View-model for the [DrivingActivity]
 */
class DrivingViewModel: ViewModel() {

    /**
     * Public TRAIL live data
     */
    val trail: MutableLiveData<Trail?> = MutableLiveData(null)
    lateinit var trailPath: TrailPath

    val lastLocation: MutableLiveData<Location?> = MutableLiveData(null)

    val trailNavigatorSituation: MutableLiveData<TrailNavigatorSituation?> = MutableLiveData(null)

    private var lastSortOrder: TrailNavigator.Direction? = null
    val sortedWayPoints = mutableListOf<MapPoint>()

    /**
     * Loads necessary trail data including waypoints.
     * Sets the selectedPoi value when complete.
     */
    fun loadTrail(trailId: Long, context: Context) {
        viewModelScope.launch {
            val service = ServiceFactory.getTrailService(context)

            val trail = service.getTrailById(trailId)
                ?: throw IllegalArgumentException("Unknown trail ID ${trailId}.")

            val trailPath = service.getTrailPath(trailId)

            // keep the order of assignments to trailPath and trail.
            // trail is an observable and we want to guarantee that trailPath exists if trail exists
            this@DrivingViewModel.trailPath = trailPath
            this@DrivingViewModel.trail.value = trail
        }
    }

    fun sortWayPoints(direction: TrailNavigator.Direction?): Boolean {
        return if (direction != lastSortOrder) {
            when (direction) {
                TrailNavigator.Direction.FORWARD -> {
                    sortedWayPoints.sortBy { point ->
                        point.navigationOrder
                            ?: throw IllegalArgumentException("The way point #${point.id} has no navigationOrder value.")
                    }
                }
                TrailNavigator.Direction.BACKWARD -> {
                    sortedWayPoints.sortByDescending { point ->
                        point.navigationOrder
                            ?: throw IllegalArgumentException("The way point #${point.id} has no navigationOrder value.")
                    }
                }
            }

            lastSortOrder = direction
            true
        } else {
            false
        }
    }
}