package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.text.HtmlCompat
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.PoiListItemBinding
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailDriveWaypoint

/**
 * View Binder for the [WaypointListAdapter]
 */
class WaypointListViewBinder(private val context: Context,
                             private var displayDescription: Boolean,
                             private var displayAction: Boolean
): ListItemAdapterViewBinder<TrailDriveWaypoint> {

    private val standardTextColor = context.getColor(R.color.colorListText)
    private val selectedTextColor = context.getColor(R.color.colorListTextSelection)
    private val subSelectedTextColor = context.getColor(R.color.colorListTextSubSelection)
    private val formatter = Formatter.getDistanceFormatter()

    private var subSelectedItemId: Long? = null

    override fun bindView(view: View, item: TrailDriveWaypoint, isSelected: Boolean, isFavorite: Boolean) {
        val binding = PoiListItemBinding.bind(view)
        // Mileage
        val distanceInMeters = item.distanceMarker
        val mileage = if (distanceInMeters == null) {
            context.getString(R.string.na)
        } else {
            formatter.formatDistance(context, distanceInMeters)
        }
        binding.poiListItemMillage.text = mileage
        // Name + description
        binding.poiListItemName.text = item.point.name
        binding.poiListItemDescription.text = HtmlCompat.fromHtml(item.descriptionShort?:"", HtmlCompat.FROM_HTML_MODE_LEGACY)
        binding.poiListItemDescription.movementMethod = LinkMovementMethod.getInstance()

        val isSubSelected = item.id == subSelectedItemId
        val textColor = if (isSelected) selectedTextColor else if (isSubSelected) subSelectedTextColor else  standardTextColor

        // Colors
        binding.poiListItemMillage.setTextColor(textColor)
        binding.poiListItemName.setTextColor(textColor)

        val descView = binding.poiListItemDescription
        if (displayDescription) {
            descView.visibility = View.VISIBLE
            descView.text =
                HtmlCompat.fromHtml(item.description ?: "", HtmlCompat.FROM_HTML_MODE_LEGACY)
            descView.movementMethod = LinkMovementMethod.getInstance()
            descView.setTextColor(textColor)
        } else {
            descView.visibility = View.GONE
        }

        binding.poiListItemAction.visibility = displayAction.visibility
    }

    override fun getViewResourceId(): Int {
        return R.layout.poi_list_item
    }

    fun setDisplayDescription(displayDescription: Boolean) {
        this.displayDescription = displayDescription
    }

    fun setSubSelectedId(id: Long?) {
        this.subSelectedItemId = id
    }

}
