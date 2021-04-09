package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import com.neotreks.accuterra.mobile.demo.ui.SimpleSpinnerListViewBinder
import com.neotreks.accuterra.mobile.sdk.trail.model.PointType

/**
 * View Binder for the [PointTypeAdapter]
 */
class PointTypeListViewBinder(context: Context): SimpleSpinnerListViewBinder<PointType>(context) {

    override fun getItemLabel(item: PointType): String {
        return item.name
    }

}