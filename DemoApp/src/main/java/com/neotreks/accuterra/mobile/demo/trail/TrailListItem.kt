package com.neotreks.accuterra.mobile.demo.trail

import com.neotreks.accuterra.mobile.sdk.trail.model.TechnicalRating
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailBasicInfo
import com.neotreks.accuterra.mobile.sdk.trail.model.UserRating

class TrailListItem(
    val id: Long,
    val title: String,
    val shortDescription: String,
    val length: Float,
    val rating: UserRating?,
    val isBookmarked: Boolean,
    val difficulty: TechnicalRating,
    val highestElevation: Float?,
    val estimatedDriveTimeMin: Int?,
    val estimatedDriveTimeAvg: Int
) {

    companion object {
        fun from(trailBasicInfo: TrailBasicInfo): TrailListItem {
            val difficulty = trailBasicInfo.techRatingHigh

            return TrailListItem(
                id = trailBasicInfo.id,
                title = trailBasicInfo.name,
                shortDescription = trailBasicInfo.locationInfo.nearestTownName ?: trailBasicInfo.highlights,
                length = trailBasicInfo.statistics.length,
                rating = trailBasicInfo.userRating,
                isBookmarked = false,
                difficulty = difficulty,
                highestElevation = trailBasicInfo.statistics.highestElevation,
                estimatedDriveTimeMin = trailBasicInfo.statistics.estimatedDriveTimeMin,
                estimatedDriveTimeAvg = trailBasicInfo.statistics.estimatedDriveTimeAvg
            )
        }
    }

    init {
        require(length > 0.0) { "The length must be greater than 0." }
    }

}