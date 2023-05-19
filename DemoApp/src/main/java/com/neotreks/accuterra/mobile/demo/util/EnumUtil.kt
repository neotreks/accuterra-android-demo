package com.neotreks.accuterra.mobile.demo.util

import android.content.Context
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.TechnicalRating

/**
 * Utility class for caching enums values.
 */
object EnumUtil {

    private val techRatings = mutableMapOf<String, TechnicalRating>()

    /**
     * Caches enum values by accessing those for a first time.
     */
    suspend fun cacheEnums(context: Context) {
        getTechRatings(context)
    }

    suspend fun getTechRatingForCode(code: String, context: Context): TechnicalRating? {
        initTechRatings(context)
        return techRatings[code]
    }

    suspend fun getTechRatings(context: Context): List<TechnicalRating> {
        initTechRatings(context)
        return techRatings.values.toList()
    }

    private suspend fun initTechRatings(context: Context) {
        if (techRatings.isEmpty()) {
            val ratings = ServiceFactory.getEnumService(context).getTechRatings()
            ratings.map { rating ->
                techRatings[rating.code] = rating
            }
        }
    }

}