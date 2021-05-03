package com.neotreks.accuterra.mobile.demo.media

import com.neotreks.accuterra.mobile.sdk.media.SdkMediaCategory
import com.neotreks.accuterra.mobile.sdk.media.SdkMediaVariant

/**
 * Utility to get media variant URL
 */
object ApkMediaVariantUtil {

    /**
     * This utility method maps media variants defined in the **DEMO APP** [ApkMediaVariant]
     * to **SDK** media variants defined by [SdkMediaCategory] and [SdkMediaVariant].
     *
     * Each implementation has its own mapping of variants.
     * Also, new variants can appear during the time.
     */
    fun getUrlForVariant(baseUrl: String, mediaCategoryNumber: Int, variant: ApkMediaVariant) : String {

        if (mediaCategoryNumber == SdkMediaCategory.TRAIL_MEDIA.number) {
            return when (variant) {
                ApkMediaVariant.DEFAULT -> {
                    SdkMediaCategory.TRAIL_MEDIA.getVariantUrl(baseUrl, SdkMediaVariant.trail)
                }
                ApkMediaVariant.THUMBNAIL -> {
                    SdkMediaCategory.TRAIL_MEDIA.getVariantUrl(baseUrl, SdkMediaVariant.trailThumbnail)
                }
            }
        }

        if (mediaCategoryNumber == SdkMediaCategory.TRAIL_MAP.number) {
            return when (variant) {
                ApkMediaVariant.DEFAULT -> {
                    SdkMediaCategory.TRAIL_MAP.getVariantUrl(baseUrl, SdkMediaVariant.trailMap)
                }
                ApkMediaVariant.THUMBNAIL -> {
                    SdkMediaCategory.TRAIL_MAP.getVariantUrl(baseUrl, SdkMediaVariant.trailMapThumbnail)
                }
            }
        }

        throw IllegalArgumentException("Unsupported media variant $variant for category number: $mediaCategoryNumber")
    }

}