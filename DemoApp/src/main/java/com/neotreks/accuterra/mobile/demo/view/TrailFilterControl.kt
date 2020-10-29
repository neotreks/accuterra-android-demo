package com.neotreks.accuterra.mobile.demo.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.util.EnumUtil
import com.neotreks.accuterra.mobile.sdk.trail.model.TechnicalRating
import kotlinx.android.synthetic.main.trail_filter.view.*
import kotlinx.coroutines.runBlocking

class TrailFilterControl(context: Context, attrs: AttributeSet):
    ConstraintLayout(context, attrs),
    SeekBar.OnSeekBarChangeListener,
    CompoundButton.OnCheckedChangeListener {

    companion object {
        private const val MAX_STARS = 5
    }

    private var listener: OnTrailFilterChangeListener? = null

    private lateinit var difficultyLevels: List<TechnicalRating>

    private val tripDistances = arrayOf(5, 10, 15, 20, 25, 30, 40, 50, 60, 70, 80, 100,
        120, 150, 200, 250, 300, 400, 500, 600, 700, 800, 900, 1000)

    init {
        runBlocking {
            difficultyLevels = EnumUtil.getTechRatings(context)
        }
    }

    var maxDifficultyLevel: TechnicalRating?
        get() {
            return getDifficultyLevelFromProgress(trail_filter_difficulty.progress)
        }
        set(value) {
            trail_filter_difficulty.progress = getProgressFromDifficulty(value)
            refreshDifficultyLabel(value)
        }

    var minUserRating: Int?
        get() {
            return getStarsFromProgress(trail_filter_user_rating.progress)
        }
        set(value) {
            trail_filter_user_rating.progress = getProgressFromStars(value)
            refreshUserRatingLabel(value)
        }

    var maxTripDistance: Int?
        get() {
            return getTripDistanceFromProgress(trail_filter_trip_distance.progress)
        }
        set(value) {
            trail_filter_trip_distance.progress = getProgressFromTripDistance(value)
            refreshTripDistanceLabel(value)
        }

    var favoriteOnly: Boolean
        get() {
            return trail_filter_trip_favorite.isChecked
        }
        set(value) {
            trail_filter_trip_favorite.isChecked = value
        }

    init {
        View.inflate(context, R.layout.trail_filter, this)

        initDifficultySeekBar()
        initUserRatingSeekBar()
        initTripDistanceSeekBar()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        trail_filter_difficulty.setOnSeekBarChangeListener(this)
        trail_filter_user_rating.setOnSeekBarChangeListener(this)
        trail_filter_trip_distance.setOnSeekBarChangeListener(this)
        trail_filter_trip_favorite.setOnCheckedChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        trail_filter_difficulty.setOnSeekBarChangeListener(null)
        trail_filter_user_rating.setOnSeekBarChangeListener(null)
        trail_filter_trip_distance.setOnSeekBarChangeListener(null)
        trail_filter_trip_favorite.setOnCheckedChangeListener(null)
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        when (seekBar) {
            trail_filter_difficulty -> {
                val newDifficulty = getDifficultyLevelFromProgress(progress)
                refreshDifficultyLabel(newDifficulty)

                if (fromUser) {
                    listener?.onMaxDifficultyLevelChanged(this, newDifficulty)
                }
            }
            trail_filter_user_rating -> {
                val newRating = getStarsFromProgress(progress)
                refreshUserRatingLabel(newRating)

                if (fromUser) {
                    listener?.onMinUserRatingChanged(this, getStarsFromProgress(progress))
                }
            }
            trail_filter_trip_distance -> {
                val newTripDistance = getTripDistanceFromProgress(progress)
                refreshTripDistanceLabel(newTripDistance)

                if (fromUser) {
                    listener?.onMaxTripDistanceChanged(this, newTripDistance)
                }
            }
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        when(buttonView) {
            trail_filter_trip_favorite -> {
                listener?.onFavoriteChanged(this, isChecked)
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    fun setOnTrailFilterChangeListener(listener: OnTrailFilterChangeListener?) {
        this.listener = listener
    }

    private fun getDifficultyLevelFromProgress(progress: Int): TechnicalRating? {
        return if (progress < difficultyLevels.size) {
            difficultyLevels[progress]
        } else {
            null
        }
    }

    private fun getProgressFromDifficulty(trailDifficulty: TechnicalRating?): Int {
        @Suppress("LiftReturnOrAssignment")
        if (trailDifficulty != null) {
            val index = difficultyLevels.indexOf(trailDifficulty)
            require(index != -1) { "Cannot set the filter to $trailDifficulty." }
            return index
        } else {
            return difficultyLevels.size
        }
    }

    private fun getStarsFromProgress(progress: Int): Int? {
        return if (progress == MAX_STARS) {
            null
        } else {
            MAX_STARS - progress
        }
    }

    private fun getProgressFromStars(starsCount: Int?): Int {
        return if (starsCount == null) {
            MAX_STARS
        } else {
            require(starsCount in 1..MAX_STARS) { "The startCount must be a value from 1 to MAX_STARS." }
            MAX_STARS - starsCount
        }
    }

    private fun getTripDistanceFromProgress(progress: Int): Int? {
        return if (progress < tripDistances.size) {
            tripDistances[progress]
        } else {
            null
        }
    }

    private fun getProgressFromTripDistance(tripDistance: Int?): Int {
        @Suppress("LiftReturnOrAssignment")
        if (tripDistance != null) {
            val index = tripDistances.indexOf(tripDistance)
            require(index != -1) { "Cannot set the filter to $tripDistance." }
            return index
        } else {
            return tripDistances.size
        }
    }

    private fun initDifficultySeekBar() {
        // trail_filter_difficulty.min = 0
        trail_filter_difficulty.max = difficultyLevels.size
        trail_filter_difficulty.progress = getProgressFromDifficulty(null)

        refreshDifficultyLabel()
    }

    private fun initUserRatingSeekBar() {
        // trail_filter_difficulty.min = 0
        trail_filter_user_rating.max = MAX_STARS
        trail_filter_user_rating.progress = getProgressFromStars(null)

        refreshUserRatingLabel()
    }

    private fun initTripDistanceSeekBar() {
        // trail_filter_trip_distance.min = 0
        trail_filter_trip_distance.max = tripDistances.size
        trail_filter_trip_distance.progress = getProgressFromTripDistance(null)

        refreshTripDistanceLabel()
    }

    private fun refreshDifficultyLabel() {
        refreshDifficultyLabel(maxDifficultyLevel)
    }

    private fun refreshDifficultyLabel(difficultyLevel: TechnicalRating?) {
        trail_filter_difficulty_value_label.text = difficultyLevel?.name
            ?: context.getString(R.string.any)
    }

    private fun refreshUserRatingLabel() {
        refreshUserRatingLabel(minUserRating)
    }

    private fun refreshUserRatingLabel(starsCount: Int?) {
        trail_filter_user_rating_value_label.text =  if (starsCount != null) {
            context.resources.getQuantityString(R.plurals.star, starsCount, starsCount)
        } else {
            context.getString(R.string.any)
        }
    }

    private fun refreshTripDistanceLabel() {
        refreshTripDistanceLabel(maxTripDistance)
    }

    private fun refreshTripDistanceLabel(tripDistance: Int?) {
        trail_filter_trip_distance_value_label.text = if (tripDistance != null) {
            context.getString(
                R.string.distance_integer_with_unit,
                tripDistance,
                context.getString(R.string.mile_unit_abbr))
        } else {
            context.getString(R.string.any)
        }
    }

    interface OnTrailFilterChangeListener {
        fun onMaxDifficultyLevelChanged(trailFilterControl: TrailFilterControl, maxDifficultyLevel: TechnicalRating?)
        fun onMinUserRatingChanged(trailFilterControl: TrailFilterControl, minUserRating: Int?)
        fun onMaxTripDistanceChanged(trailFilterControl: TrailFilterControl, maxTripDistance: Int?)
        fun onFavoriteChanged(trailFilterControl: TrailFilterControl, favoriteOnly: Boolean)
    }
}