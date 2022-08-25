package com.neotreks.accuterra.mobile.demo.util

import android.content.Context
import com.neotreks.accuterra.mobile.demo.trail.AccuTerraSatelliteStyleProvider
import com.neotreks.accuterra.mobile.sdk.map.style.AccuterraStyleProvider
import com.neotreks.accuterra.mobile.sdk.map.style.IAccuTerraStyleProvider

/**
 * Utility methods for APK activities
 */
object ApkMapActivityUtil {

    /**
     * Get the [IAccuTerraStyleProvider] to be able to customize styles
     * of trail paths, waypoints, markers, etc.
     */
    fun getStyleProvider(style: String, context: Context): IAccuTerraStyleProvider? {
        if (isSatellite(style)) {
            return AccuTerraSatelliteStyleProvider(context)
        }
        return AccuterraStyleProvider(context)
    }

    private fun isSatellite(style: String) = style.contains("satellite")

}