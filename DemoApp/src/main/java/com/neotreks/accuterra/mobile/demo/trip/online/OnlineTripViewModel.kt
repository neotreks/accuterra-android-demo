package com.neotreks.accuterra.mobile.demo.trip.online

import android.app.Activity
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.sdk.trip.model.Trip
import com.neotreks.accuterra.mobile.sdk.ugc.model.SetTripLikedResult
import com.neotreks.accuterra.mobile.sdk.ugc.model.TripComment
import kotlinx.coroutines.CancellationException

/**
 * View model for the [OnlineTripActivity]
 */
class OnlineTripViewModel : ViewModel() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "OnlineTripViewModel"
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    var tripUuid: String = ""

    var selectedMediaUrl: String? = null
    var selectedPoiUuid: String? = null

    val trip: MutableLiveData<Trip?> = MutableLiveData()
    val comments: MutableLiveData<List<TripComment>?> = MutableLiveData()
    val userData: MutableLiveData<SetTripLikedResult?> = MutableLiveData()

    var commentsUpdated = false

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    suspend fun loadTrip(context: Activity, uuid: String) {
        val result = OnlineTripRepo().loadTrip(context, uuid)
        if (result.isSuccess) {
            val trip = result.value!!
            this.trip.value = trip
            userData.value = SetTripLikedResult(
                trip.info.uuid,
                trip.userInfo.userLike ?: false,
                trip.likesCount
            )
        } else {
            if (result.error is CancellationException) {
                Log.d(TAG, "Trip load cancelled: $uuid")
            } else {
                val errorMessage = "Error while loading trip: $uuid, ${result.buildErrorMessage()}"
                Log.e(TAG, errorMessage, result.error ?: Exception())
                context.longToast(errorMessage)
            }
            trip.value = null
            userData.value = null
        }
    }

    suspend fun loadTripComments(context: Activity, tripUuid: String) {
        val result = OnlineTripRepo().loadTripComments(context, tripUuid)
        if (result.isSuccess) {
            if (result.value == null) {
                context.longToast("No value in trip comments result.")
            } else {
                comments.postValue(result.value?.comments)
            }
        } else {
            if (result.error is CancellationException) {
                Log.d(TAG, "Trip comments load cancelled: $tripUuid")
            } else {
                val errorMessage = "Error while loading trip comments: $tripUuid, ${result.buildErrorMessage()}"
                Log.e(TAG, errorMessage, result.error ?: Exception())
                context.longToast(errorMessage)
            }
            trip.postValue(null)
        }
    }

    fun reset() {
        tripUuid = ""
        selectedMediaUrl = null
        selectedPoiUuid = null
        trip.value = null
        comments.value = null
    }

    fun isTripLoaded(): Boolean {
        return trip.value != null
    }

    fun updateTripCommentsCount(commentsCount: Int?) {
        if (commentsCount == null) return
        val trip = this.trip.value ?: return
        // Check if there was a change and propagate it
        if (trip.commentsCount != commentsCount) {
            val newTrip = trip.copy(commentsCount = commentsCount)
            this.trip.value = newTrip
            commentsUpdated = true
        }
    }

    fun setUserLike(likesResult: SetTripLikedResult) {
        val userDataNew = userData.value?.copy(
            userLike = likesResult.userLike,
            likes = likesResult.likes
        )
        this.userData.value = userDataNew
    }

}