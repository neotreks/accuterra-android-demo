package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi
import kotlinx.android.synthetic.main.trip_poi_list_item.view.*

/**
 * View Binder for the [RecordedTripPoiListAdapter]
 */
class RecordedTripPoiListViewBinder(private val context: Context) : ListItemAdapterViewBinder<TripRecordingPoi> {

    override fun bindView(view: View, item: TripRecordingPoi, isSelected: Boolean) {
        // Type
        view.trip_poi_list_item_type.text = item.pointSubtype.code
        // Name + description
        view.trip_poi_list_item_name.text = item.name
        view.trip_poi_list_item_description.text = item.description
    }

    override fun getViewResourceId(): Int {
        return R.layout.trip_poi_list_item
    }

}