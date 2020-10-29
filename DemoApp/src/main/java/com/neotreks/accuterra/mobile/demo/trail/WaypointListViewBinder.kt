package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.text.HtmlCompat
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailDriveWaypoint
import kotlinx.android.synthetic.main.poi_list_item.view.*

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

    override fun bindView(view: View, item: TrailDriveWaypoint, isSelected: Boolean) {
        // Mileage
        val distanceInMeters = item.distanceMarker
        val mileage = if (distanceInMeters == null) {
            context.getString(R.string.na)
        } else {
            formatter.formatDistance(context, distanceInMeters)
        }
        view.poi_list_item_millage.text = mileage
        // Name + description
        view.poi_list_item_name.text = item.point.name
        view.poi_list_item_description.text = HtmlCompat.fromHtml(item.descriptionShort?:"", HtmlCompat.FROM_HTML_MODE_LEGACY)
        view.poi_list_item_description.movementMethod = LinkMovementMethod.getInstance()

        val isSubSelected = item.id == subSelectedItemId
        val textColor = if (isSelected) selectedTextColor else if (isSubSelected) subSelectedTextColor else  standardTextColor

        view.poi_list_item_millage.setTextColor(textColor)
        view.poi_list_item_name.setTextColor(textColor)

        val descView = view.poi_list_item_description
        if (displayDescription) {
            descView.visibility = View.VISIBLE
            descView.text =
                HtmlCompat.fromHtml(item.description ?: "", HtmlCompat.FROM_HTML_MODE_LEGACY)
            descView.movementMethod = LinkMovementMethod.getInstance()
            descView.setTextColor(textColor)
        } else {
            descView.visibility = View.GONE
        }

        view.poi_list_item_action.visibility = displayAction.visibility
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
