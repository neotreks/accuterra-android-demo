package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailDriveWaypoint
import kotlinx.coroutines.launch

/**
 * View model class for the [TrailPoiDetailActivity]
 */
class TrailPoiDetailViewModel : ViewModel() {

    val poi = MutableLiveData<TrailDriveWaypoint?>(null)

    fun loadPoi(context: Context, waypointId: Long) {
        viewModelScope.launch {
            val repo = TrailPoiDetailRepo(context)
            val repoPoi = repo.getWaypoint(waypointId)
            poi.value = repoPoi
        }
    }

}