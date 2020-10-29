package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import com.neotreks.accuterra.mobile.demo.ui.SimpleSpinnerListViewBinder
import com.neotreks.accuterra.mobile.sdk.trail.model.PointSubtype

/**
 * View Binder for the [PointSubtypeAdapter]
 */
class PointSubtypeListViewBinder(context: Context): SimpleSpinnerListViewBinder<PointSubtype>(context) {

    override fun getItemLabel(item: PointSubtype): String {
        return item.name
    }

}