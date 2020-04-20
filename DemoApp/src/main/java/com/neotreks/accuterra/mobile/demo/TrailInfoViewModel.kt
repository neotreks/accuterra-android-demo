package com.neotreks.accuterra.mobile.demo

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.TechnicalRating
import com.neotreks.accuterra.mobile.sdk.trail.model.Trail
import kotlinx.coroutines.launch

/**
 * View model for the [TrailInfoActivity]
 */
class TrailInfoViewModel: ViewModel() {

    var trailId: Long = Long.MIN_VALUE

    var trail = MutableLiveData<Trail>()
        private set

    val imageS3Keys = mutableListOf<String>()

    var techRatings: List<TechnicalRating> = listOf()
        private set

    fun loadTechRatings(context: Context) {
        viewModelScope.launch {
            techRatings = ServiceFactory.getEnumService(context).getTechRatings()
        }
    }

    @Throws(IllegalStateException::class)
    fun loadTrail(trailId: Long, context: Context) {
        viewModelScope.launch {
            val trailObject = ServiceFactory.getTrailService(context).getTrailById(trailId)
                    ?: throw IllegalArgumentException("Unknown trail ID ${trailId}.")
            trail.value = trailObject
        }
    }

}