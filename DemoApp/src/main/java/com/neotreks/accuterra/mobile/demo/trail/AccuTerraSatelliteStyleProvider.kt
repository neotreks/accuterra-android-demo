package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyValue
import com.neotreks.accuterra.mobile.sdk.map.style.AccuterraStyleProvider
import com.neotreks.accuterra.mobile.sdk.map.style.TrailLayerStyleType

/**
 * A custom style provider used for imaginary base map
 */
class AccuTerraSatelliteStyleProvider(context: Context): AccuterraStyleProvider(context) {

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

    override fun getFontStack(): Array<String> {
        return arrayOf("DIN Offc Pro Italic","Arial Unicode MS Regular")
    }
}