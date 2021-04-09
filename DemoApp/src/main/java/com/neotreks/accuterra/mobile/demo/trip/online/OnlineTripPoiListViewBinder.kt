package com.neotreks.accuterra.mobile.demo.trip.online

import android.content.Context
import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.TripPoiListItemBinding
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripPoint

/**
 * View Binder for the [OnlineTripPoiListAdapter]
 */
class OnlineTripPoiListViewBinder(private val context: Context) : ListItemAdapterViewBinder<TripPoint> {

    override fun bindView(view: View, item: TripPoint, isSelected: Boolean, isFavorite: Boolean) {
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