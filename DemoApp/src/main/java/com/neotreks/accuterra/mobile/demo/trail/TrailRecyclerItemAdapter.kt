package com.neotreks.accuterra.mobile.demo.trail

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.recyclerview.widget.RecyclerView
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.TrailListItemBinding
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import kotlin.math.round
import androidx.core.graphics.toColorInt
import com.neotreks.accuterra.mobile.demo.settings.ElevationFormatter

interface TrailRecyclerItemOnClickListener {
    fun onTrailRecyclerItemClick(trailListItem: TrailListItem)
}

@SuppressLint("NotifyDataSetChanged")
class TrailRecyclerItemAdapter(
    private var trails: List<TrailListItem>,
    private val listener: TrailRecyclerItemOnClickListener
) : RecyclerView.Adapter<TrailRecyclerItemAdapter.TrailRecyclerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrailRecyclerViewHolder {
        val binding = TrailListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrailRecyclerViewHolder(binding, listener)
    }

    override fun onBindViewHolder(holder: TrailRecyclerViewHolder, position: Int) {
        holder.bind(trails[position])
    }

    override fun getItemCount(): Int = trails.size

    @UiThread
    @Synchronized
    fun setItems(items: List<TrailListItem>) {
        trails = items
        notifyDataSetChanged()
    }

    @UiThread
    fun selectTrail(trailId: Long?) {
        // TODO
    }

    class TrailRecyclerViewHolder(
        private val binding: TrailListItemBinding,
        private val listener: TrailRecyclerItemOnClickListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(trailItem: TrailListItem) {
            val context = binding.root.context
            val colorId = TechRatingColorMapper.getTechRatingColor(trailItem.difficulty.code)
            val distance = Formatter.getDistanceFormatter().formatDistance(context, trailItem.length)
            val elevation = trailItem.highestElevation?.let {
                Formatter.getElevationFormatter().formatElevation(context, it.toDouble())
            } ?: "-- FT"
            val difficultyCapsuleDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 50f
                setColor(context.getColor(colorId))
            }
            val avgTime = when {
                trailItem.estimatedDriveTimeMin != null -> {
                    Formatter.getDrivingTimeFormatter().formatEstimatedDrivingTimeRange(
                        minTimeInSeconds = trailItem.estimatedDriveTimeMin,
                        avgTimeInSeconds = trailItem.estimatedDriveTimeAvg
                    )
                }
                else -> {
                    Formatter.getDrivingTimeFormatter().formatDrivingTime(
                        context = context,
                        timeInSeconds = trailItem.estimatedDriveTimeAvg
                    )
                }
            }

            binding.trailListItemDifficultyLevel.setImageResource(colorId)
            binding.trailListItemTitle.text = trailItem.title
            binding.trailListItemBookmarkUnchecked.visibility = if (trailItem.isBookmarked) View.GONE else View.VISIBLE
            binding.trailListItemBookmarkChecked.visibility = if (trailItem.isBookmarked) View.VISIBLE else View.GONE
            binding.trailListItemLength.text = distance
            binding.trailListItemDescription.text = trailItem.shortDescription
            binding.trailListItemDifficulty.text = trailItem.difficulty.name
            binding.trailListItemDifficulty.background = difficultyCapsuleDrawable
            binding.trailListItemElevation.text = elevation
            binding.trailListItemTime.text = avgTime

            binding.root.setOnClickListener {
                listener.onTrailRecyclerItemClick(trailItem)
            }
        }
    }
}