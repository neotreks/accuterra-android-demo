package com.neotreks.accuterra.mobile.demo

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.*
import com.neotreks.accuterra.mobile.sdk.ugc.model.TrailComment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.lang.Exception

/**
 * View model for the [TrailInfoActivity]
 */
class TrailInfoViewModel: ViewModel() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "TrailInfoViewModel"
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    var trailId: Long = Long.MIN_VALUE

    var trail = MutableLiveData<Trail>()
        private set

    var imageUrls = MutableLiveData<List<TrailMedia>>()

    var drive = MutableLiveData<TrailDrive>()

    var techRatings: List<TechnicalRating> = listOf()
        private set

    val comments: MutableLiveData<List<TrailComment>?> = MutableLiveData()

    var commentsCount: MutableLiveData<Int> = MutableLiveData(0)

    val userData: MutableLiveData<TrailUserData?> = MutableLiveData()

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun isTrailIdSet(): Boolean {
        return trailId != Long.MIN_VALUE
    }

    fun loadTechRatings(context: Context) {
        viewModelScope.launch {
            techRatings = ServiceFactory.getEnumService(context).getTechRatings()
        }
    }

    @Throws(IllegalStateException::class)
    fun loadTrail(trailId: Long, context: Activity) {
        viewModelScope.launch {
            try {
                val trailService = ServiceFactory.getTrailService(context)
                val trailObject = trailService.getTrailById(trailId)
                // Do not continue if trail cannot be found (e.g. after deletion/update)
                if (trailObject == null) {
                    trail.value = null
                    return@launch
                }
                // Trail
                trail.value = trailObject
                // Drive
                drive.value = trailService.getTrailDrives(trailId).first()
                // User data
                loadTrailUserData(context, trailId)
                // Comments
                loadTrailComments(context, trailId)
                // Medias as last
                // List trail and point media URls, use thumbnails if possible
                val mediaList = trailObject.media.toMutableList()
                for (media in trailObject.navigationInfo.mapPoints.map { it.media }) {
                    mediaList.addAll(media)
                }
                // Images
                imageUrls.value = mediaList
            } catch (e: Exception) {
                Log.e(TAG, "Error while loading trail: ${e.localizedMessage}", e)
                Toast.makeText(context, context.getString(R.string.trail_errors_load_trail_failed_because_of,
                    e.localizedMessage), Toast.LENGTH_LONG).show()
                // Exit the activity - we cannot continue and use it
                context.finish()
            }
        }
    }

    suspend fun loadTrailComments(context: Activity, trailId: Long) {
        val result = TrailInfoRepo().loadTrailComments(context, trailId)
        if (result.isSuccess) {
            if (result.value == null) {
                context.longToast("No value in trail comments result.")
            } else {
                comments.value = result.value?.comments
                commentsCount.value = result.value!!.paging.total
            }
        } else {
            if (result.error is CancellationException) {
                Log.d(TAG, "Trail comments load cancelled: $trailId")
            } else {
                val errorMessage = "Error while loading trail comments: $trailId, ${result.buildErrorMessage()}"
                Log.e(TAG, errorMessage, result.error ?: Exception())
                context.toast(errorMessage)
            }
            comments.value = null
        }

    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private suspend fun loadTrailUserData(context: Activity, trailId: Long) {
        val result = TrailInfoRepo().loadTrailUserData(context, trailId)
        if (result.isSuccess) {
            if (result.value == null) {
                context.longToast("No value in trail user data result.")
            } else {
                userData.value = result.value
            }
        } else {
            if (result.error is CancellationException) {
                Log.d(TAG, "Trail user data load cancelled: $trailId")
            } else {
                val errorMessage = "Error while loading trail user data: $trailId, ${result.buildErrorMessage()}"
                Log.e(TAG, errorMessage, result.error ?: Exception())
                context.longToast(errorMessage)
            }
            userData.value = null
        }
    }

}