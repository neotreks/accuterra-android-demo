package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.TripPoiListItemBinding
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi

/**
 * View Binder for the [RecordedTripPoiListAdapter]
 */
class RecordedTripPoiListViewBinder(private val context: Context) : ListItemAdapterViewBinder<TripRecordingPoi> {

    private val poiNameFormatter by lazy {
        TripPoiNameFormatter(context)
    }

    override fun bindView(view: View, item: TripRecordingPoi, isSelected: Boolean, isFavorite: Boolean) {
        val binding = TripPoiListItemBinding.bind(view)
        // Type
        binding.tripPoiListItemType.text = item.pointType.name
        // Name + description
        binding.tripPoiListItemName.text = poiNameFormatter.getPoiNameWithDistanceMarker(item)
        binding.tripPoiListItemDescription.text = item.description
    }

    override fun getViewResourceId(): Int {
        return R.layout.trip_poi_list_item
    }

}