package com.neotreks.accuterra.mobile.demo.trail

import com.neotreks.accuterra.mobile.sdk.trail.model.TechnicalRating
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailBasicInfo
import com.neotreks.accuterra.mobile.sdk.trail.model.UserRating

class TrailListItem(
    val id: Long,
    val title: String,
    val shortDescription: String,
    val length: Double,
    val rating: UserRating?,
    val isBookmarked: Boolean,
    val difficulty: TechnicalRating
) {

    companion object {
        fun from(trailBasicInfo: TrailBasicInfo): TrailListItem {
            val difficulty = trailBasicInfo.techRatingHigh

            return TrailListItem(
                trailBasicInfo.id,
                trailBasicInfo.name,
                trailBasicInfo.highlights,
                trailBasicInfo.length * 1000, // convert kilometers to meters
                trailBasicInfo.userRating,
                false,
                difficulty
            )
        }
    }

    init {
        require(length > 0.0) { "The length must be greater than 0." }
    }

}