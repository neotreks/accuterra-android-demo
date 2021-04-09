package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.TripPoiListItemBinding
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi

/**
 * View Binder for the [RecordedTripPoiListAdapter]
 */
class RecordedTripPoiListViewBinder : ListItemAdapterViewBinder<TripRecordingPoi> {

    override fun bindView(view: View, item: TripRecordingPoi, isSelected: Boolean, isFavorite: Boolean) {
        val binding = TripPoiListItemBinding.bind(view)
        // Type
        binding.tripPoiListItemType.text = item.pointType.name
        // Name + description
        binding.tripPoiListItemName.text = item.name
        binding.tripPoiListItemDescription.text = item.description
    }

    override fun getViewResourceId(): Int {
        return R.layout.trip_poi_list_item
    }

}