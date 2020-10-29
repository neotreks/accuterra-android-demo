package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.ui.SimpleSpinnerListViewBinder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripSharingType

/**
 * View Binder for the [TripSharingTypeAdapter]
 */
class TripSharingTypeListViewBinder(private val context: Context): SimpleSpinnerListViewBinder<TripSharingType>(context) {

    override fun getItemLabel(item: TripSharingType): String {
        return when(item) {
            TripSharingType.PRIVATE -> context.getString(R.string.general_sharing_private)
            TripSharingType.FRIENDS_ONLY -> context.getString(R.string.general_sharing_friends_only)
            TripSharingType.PUBLIC -> context.getString(R.string.general_sharing_public)
            else -> {
                throw IllegalArgumentException("Unsupported TripSharingType: :$item")
            }
        }
    }

}