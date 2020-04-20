package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.UiThread
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import kotlinx.android.synthetic.main.trail_list_item.view.*
import kotlin.math.round

class TrailListItemAdapter(context: Context,
                           trails: List<TrailListItem>,
                           showActionIcons: Boolean)
    : ArrayAdapter<TrailListItem>(context, R.layout.trail_list_item, trails.toMutableList()) {

    private var listener: OnTrailListItemIconClickedListener? = null
    private val inflater = LayoutInflater.from(context)

    private var selectedTrailId: Long? = null

    private var showActionIconsBackingField = showActionIcons
    var showActionIcons: Boolean
        get() = showActionIconsBackingField
        set(value) {
            if (value != showActionIconsBackingField) {
                showActionIconsBackingField = value
                notifyDataSetChanged()
            }
        }

    @UiThread
    fun selectTrail(trailId: Long?) {
        selectedTrailId = trailId
        notifyDataSetChanged()
    }

    @UiThread
    @Synchronized
    fun setItems(items: List<TrailListItem>) {
        clear()
        addAll(items)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: inflater.inflate(R.layout.trail_list_item, parent, false)

        val trail = getItem(position)!!

        val colorId = TechRatingColorMapper.getTechRatingColor(trail.difficulty.code)
        view.trail_list_item_difficulty_level.setImageResource(colorId)

        view.trail_list_item_title.text = trail.title

        view.trail_list_item_bookmark_unchecked.visibility = if (trail.isBookmarked) View.GONE else View.VISIBLE
        view.trail_list_item_bookmark_checked.visibility = if (trail.isBookmarked) View.VISIBLE else View.GONE

        if (trail.rating == null) {
            view.trail_list_item_rating_stars.visibility = View.GONE
            view.trail_list_item_rating_stars.progress = 0
            view.trail_list_item_rating_count.text = null
            view.trail_list_item_no_rating.visibility = View.VISIBLE
        } else {
            view.trail_list_item_rating_stars.visibility = View.VISIBLE
            view.trail_list_item_rating_stars.rating = (round(trail.rating.rating * 10) / 10).toFloat()
            view.trail_list_item_rating_count.text = view.context.getString(R.string.rating_count_number, trail.rating.ratingCount)
            view.trail_list_item_no_rating.visibility = View.GONE
        }

        view.trail_list_item_length.text = Formatter.getDistanceFormatter()
            .formatDistance(view.context, trail.length)

        view.trail_list_item_short_description.text = trail.shortDescription

        attachClickHandlers(view, trail)

        view.setBackgroundResource(
            if (trail.id == selectedTrailId)
                R.color.colorListSelection
            else
                0
        )

        view.trail_list_item_icons_wrapper.visibility = if (showActionIcons) View.VISIBLE else View.GONE

        return view
    }

    fun setOnTrailListItemIconClickedListener(listener: OnTrailListItemIconClickedListener) {
        this.listener = listener
    }

    private fun attachClickHandlers(view: View, trail: TrailListItem) {
        view.trail_list_item_zoom_to_trail.setOnClickListener {
            listener?.onZoomToTrailIconClicked(trail)
        }

        view.trail_list_item_show_trail_info_screen.setOnClickListener {
            listener?.onShowTrailInfoIconClicked(trail)
        }
    }

    interface OnTrailListItemIconClickedListener {
        fun onZoomToTrailIconClicked(trailListItem: TrailListItem)
        fun onShowTrailInfoIconClicked(trailListItem: TrailListItem)
    }
}