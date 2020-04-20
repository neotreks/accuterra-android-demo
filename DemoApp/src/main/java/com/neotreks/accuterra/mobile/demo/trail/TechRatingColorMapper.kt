package com.neotreks.accuterra.mobile.demo.trail

import androidx.annotation.ColorRes
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.sdk.trail.model.SdkTechRating
import java.util.*

class TechRatingColorMapper {

    companion object {

        @ColorRes
        fun getTechRatingColor(techRatingCode: String): Int {
            return when (techRatingCode.toUpperCase(Locale.getDefault())) {
                SdkTechRating.R1.code -> R.color.difficulty_piece_of_cake
                SdkTechRating.R2.code -> R.color.difficulty_easy
                SdkTechRating.R3.code -> R.color.difficulty_medium
                SdkTechRating.R4.code -> R.color.difficulty_advanced
                SdkTechRating.R5.code -> R.color.difficulty_hard
                else -> R.color.difficulty_unknown
            }
        }
    }
}