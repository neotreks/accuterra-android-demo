package com.neotreks.accuterra.mobile.demo.trip.trailcollection

import android.content.Context
import com.neotreks.accuterra.mobile.demo.ui.SimpleSpinnerListViewBinder
import com.neotreks.accuterra.mobile.sdk.trail.model.TechnicalRating

/**
 * View Binder for the [TrailTechRatingAdapter]
 */
class TrailTechRatingListViewBinder(private val context: Context): SimpleSpinnerListViewBinder<TechnicalRating>(context) {

    override fun getItemLabel(item: TechnicalRating): String {
        return item.name
    }

}