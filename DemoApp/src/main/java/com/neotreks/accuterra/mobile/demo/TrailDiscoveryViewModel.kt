package com.neotreks.accuterra.mobile.demo

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.demo.trail.TrailListItem
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.*
import com.neotreks.accuterra.mobile.sdk.trail.model.MapBounds.Companion.MAX_LATITUDE
import com.neotreks.accuterra.mobile.sdk.trail.model.MapBounds.Companion.MAX_LONGITUDE
import com.neotreks.accuterra.mobile.sdk.trail.model.MapBounds.Companion.MIN_LATITUDE
import com.neotreks.accuterra.mobile.sdk.trail.model.MapBounds.Companion.MIN_LONGITUDE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View Model for the [TrailDiscoveryActivity]
 */
class TrailDiscoveryViewModel: ViewModel() {

    var trailNameFilter: String? = null
    var maxDifficultyLevel: TechnicalRating? = null
    var minUserRating: Int? = null
    var maxTripDistance: Int? = null
    var favoriteOnly: Boolean = false

    lateinit var defaultTitle: CharSequence

    /**
     * Public TRAIL live data
     */
    val trailsListItems = MutableLiveData<List<TrailListItem>>()

    /**
     * Selected trail ID
     */
    var selectedTrailId: Long? = null

    init {
        trailsListItems.value = listOf()
    }

    var lastSearchedMapBounds = MapBounds(MIN_LATITUDE, MIN_LONGITUDE, MAX_LATITUDE, MAX_LONGITUDE)

    /**
     * Find trails by [TrailMapBoundsSearchCriteria]
     */
    fun findTrailsByMapBoundsAsync(
        criteria: TrailMapBoundsSearchCriteria,
        context: Context,
        onSuccessCallback: (Iterable<TrailBasicInfo>) -> Unit
    ) {
        findTrailsImpl(criteria, context, onSuccessCallback)
    }

    /**
     * Find trails by [TrailMapSearchCriteria]
     */
    fun findTrailsByMapCenterAsync(
        criteria: TrailMapSearchCriteria,
        context: Context,
        onSuccessCallback: (Iterable<TrailBasicInfo>) -> Unit
    ) {
        findTrailsImpl(criteria, context, onSuccessCallback)
    }

    private fun findTrailsImpl(
        criteria: Any,
        context: Context,
        onSuccessCallback: (Iterable<TrailBasicInfo>) -> Unit
    ) {
        // Cancel previous search
        viewModelScope.coroutineContext.cancelChildren()
        // Run new search
        viewModelScope.launch(Dispatchers.IO) {
            val service = ServiceFactory.getTrailService(context)
            val trails = when(criteria) {
                is TrailMapSearchCriteria -> service.findTrails(criteria)
                is TrailMapBoundsSearchCriteria -> service.findTrails(criteria)
                else -> throw IllegalArgumentException("Unsupported criteria type.")
            }
            val items = trails.map { TrailListItem.from(it) }
            setTrailsList(items)
            withContext(Dispatchers.Main) {
                onSuccessCallback(trails)
            }
        }
    }

    @Synchronized
    fun setTrailsList(newTrailsListItems: List<TrailListItem>) {
        // We need to launch this in the main thread
        viewModelScope.launch {
            trailsListItems.value = newTrailsListItems
        }
    }

}