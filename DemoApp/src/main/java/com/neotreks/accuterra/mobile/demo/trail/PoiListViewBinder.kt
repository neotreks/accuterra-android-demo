package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.sdk.trail.model.MapPoint
import kotlinx.android.synthetic.main.poi_list_item.view.*

/**
 * View Binder for the [PoiListAdapter]
 */
class PoiListViewBinder(private val context: Context): ListItemAdapterViewBinder<MapPoint> {

    private val standardTextColor = context.getColor(R.color.colorListText)
    private val selectedTextColor = context.getColor(R.color.colorListTextSelection)
    private val formatter = Formatter.getDistanceFormatter()

    override fun bindView(view: View, item: MapPoint, isSelected: Boolean) {
        // Mileage
        val distanceInMeters = item.distanceMarker
        val mileage = if (distanceInMeters == null) {
            context.getString(R.string.na)
        } else {
            formatter.formatDistance(context, distanceInMeters)
        }
        view.poi_list_item_millage.text = mileage
        // Name + description
        view.poi_list_item_name.text = item.name
        view.poi_list_item_description.text = item.description

        val textColor = if (isSelected) selectedTextColor else  standardTextColor

        view.poi_list_item_millage.setTextColor(textColor)
        view.poi_list_item_name.setTextColor(textColor)
        view.poi_list_item_description.setTextColor(textColor)

    }

    override fun getViewResourceId(): Int {
        return R.layout.poi_list_item
    }

}
