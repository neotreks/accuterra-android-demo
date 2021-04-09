package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.UiThread
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.TrailListItemBinding
import com.neotreks.accuterra.mobile.demo.settings.Formatter
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
        val binding = TrailListItemBinding.bind(view)

        val trail = getItem(position)!!

        val colorId = TechRatingColorMapper.getTechRatingColor(trail.difficulty.code)
        binding.trailListItemDifficultyLevel.setImageResource(colorId)

        binding.trailListItemTitle.text = trail.title

        binding.trailListItemBookmarkUnchecked.visibility = if (trail.isBookmarked) View.GONE else View.VISIBLE
        binding.trailListItemBookmarkChecked.visibility = if (trail.isBookmarked) View.VISIBLE else View.GONE

        if (trail.rating == null) {
            binding.trailListItemRatingStars.visibility = View.GONE
            binding.trailListItemRatingStars.progress = 0
            binding.trailListItemRatingCount.text = null
            binding.trailListItemNoRating.visibility = View.VISIBLE
        } else {
            binding.trailListItemRatingStars.visibility = View.VISIBLE
            binding.trailListItemRatingStars.rating = (round(trail.rating.rating * 10) / 10)
            binding.trailListItemRatingCount.text = view.context.getString(R.string.rating_count_number, trail.rating.ratingCount)
            binding.trailListItemNoRating.visibility = View.GONE
        }

        binding.trailListItemLength.text = Formatter.getDistanceFormatter()
            .formatDistance(view.context, trail.length)

        binding.trailListItemShortDescription.text = trail.shortDescription

        attachClickHandlers(view, trail)

        view.setBackgroundResource(
            if (trail.id == selectedTrailId)
                R.color.colorListSelection
            else
                0
        )

        binding.trailListItemIconsWrapper.visibility = if (showActionIcons) View.VISIBLE else View.GONE

        return view
    }

    fun setOnTrailListItemIconClickedListener(listener: OnTrailListItemIconClickedListener) {
        this.listener = listener
    }

    private fun attachClickHandlers(view: View, trail: TrailListItem) {
        val binding = TrailListItemBinding.bind(view)
        binding.trailListItemZoomToTrail.setOnClickListener {
            listener?.onZoomToTrailIconClicked(trail)
        }

        binding.trailListItemShowTrailInfoScreen.setOnClickListener {
            listener?.onShowTrailInfoIconClicked(trail)
        }
    }

    interface OnTrailListItemIconClickedListener {
        fun onZoomToTrailIconClicked(trailListItem: TrailListItem)
        fun onShowTrailInfoIconClicked(trailListItem: TrailListItem)
    }
}