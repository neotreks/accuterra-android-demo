package com.neotreks.accuterra.mobile.demo.trip.trailcollection

import android.content.Context
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapter
import com.neotreks.accuterra.mobile.sdk.trail.model.TechnicalRating


/**
 *  Adapter for list of sharing types
 */
class TrailTechRatingAdapter(context: Context, techRatings: List<TechnicalRating>) : ListItemAdapter<TechnicalRating>(
    context,
    techRatings,
    TrailTechRatingListViewBinder(context)
) {
    override fun getItemId(item: TechnicalRating): Long {
        return item.level.toLong()
    }
}