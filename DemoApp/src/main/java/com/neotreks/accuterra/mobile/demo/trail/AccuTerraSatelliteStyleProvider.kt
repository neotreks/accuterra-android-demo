package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import android.util.Log
import com.mapbox.geojson.Feature
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.map.style.AccuterraStyleProvider
import com.neotreks.accuterra.mobile.sdk.map.style.TrailLayerStyleType
import com.neotreks.accuterra.mobile.sdk.map.style.TrailPoiStyleType
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailBasicInfo
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailMarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A custom style provider used for imaginary base map
 */
class AccuTerraSatelliteStyleProvider(val context: Context): AccuterraStyleProvider(context) {

    override fun getTrailProperties(type: TrailLayerStyleType): Array<PropertyValue<out Any>> {
        return when(type) {
            // TRAIL PATH
            TrailLayerStyleType.TRAIL_PATH -> {
                // we have to make it visible on dar background
                arrayOf(PropertyFactory.lineColor("rgb(255,215,0)"))
            }
            // Other keep as default
            else -> super.getTrailProperties(type)
        }
    }

    /**
     * This method demonstrates how a poi can be styled using a custom attribute
     * that we created in `modifyFeature`
     */
    override fun getPoiProperties(type: TrailPoiStyleType): Array<PropertyValue<out Any>> {
        return when(type) {
            TrailPoiStyleType.TRAIL_HEAD -> arrayOf(
                PropertyFactory.iconSize(1.0f),
                PropertyFactory.iconColor(
                    Expression.match(
                        Expression.get("difficulty"),
                        Expression.literal("1"), Expression.rgba(0.0f, 190.0f, 10.0f, 1.0f),
                        Expression.literal("2"), Expression.rgba(0.0f, 0.0f, 255.0f, 1.0f),
                        Expression.literal("3"), Expression.rgba(255.0f, 171.0f, 0.0f, 1.0f),
                        Expression.literal("4"), Expression.rgba(255.0f, 0.0f, 0.0f, 1.0f),
                        Expression.literal("5"), Expression.rgba(255.0f, 255.0f, 255.0f, 1.0f),
                        Expression.rgba(0.0f, 0.0f, 0.0f, 1.0f)
                    )
                ),
                PropertyFactory.iconIgnorePlacement(true), // VERY IMPORTANT!!
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconOpacity(0.85f),
                PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_VIEWPORT),
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                PropertyFactory.iconTranslateAnchor(Property.ICON_TRANSLATE_ANCHOR_VIEWPORT),
            )
            TrailPoiStyleType.TRAIL_POI,
            TrailPoiStyleType.SELECTED_TRAIL_POI ->
                super.getPoiProperties(type)
        }
    }

    override fun getFontStack(): Array<String> {
        return arrayOf("DIN Offc Pro Italic","Arial Unicode MS Regular")
    }

    /**
     * This method demonstrates how a feature can be modified. Check `getPoiProperties` to see how the
     * attribute is used.
     */
    override suspend fun modifyFeature(feature: Feature, marker: TrailMarker) {
        withContext(Dispatchers.IO) {
            val trailBasicInfo = ServiceFactory.getTrailService(context).getTrailBasicInfoById(marker.trailId)
            val difficulty = getTrailDifficulty(trailBasicInfo)
            feature.addStringProperty("difficulty", difficulty)
            Log.d("modifyFeature:", feature.toJson())
        }
    }

    private fun getTrailDifficulty(trail: TrailBasicInfo?):String{
        val difficultyLevel = trail?.techRatingHigh?.level
        return difficultyLevel.toString()
    }
}