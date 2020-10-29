package com.neotreks.accuterra.mobile.demo

import android.content.Context
import android.content.SharedPreferences
import com.neotreks.accuterra.mobile.demo.util.jsonToClass
import com.neotreks.accuterra.mobile.demo.util.toJson
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraStyle
import com.neotreks.accuterra.mobile.sdk.map.MapSetupSnapshot

class DemoAppPreferences {

    companion object {

        /* * * * * * * * * * * * */
        /*      PROPERTIES       */
        /* * * * * * * * * * * * */

        private const val KEY_REQUESTING_LOCATION = "demo_requesting_loc"

        private const val PREF_FILE_NAME = "DemoAppPreferences"

        /* * * * * * * * * * * * */
        /*        PUBLIC         */
        /* * * * * * * * * * * * */

        fun setRequestingLocation(context: Context, requestingLocation: Boolean) {
            getPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION, requestingLocation)
                .apply()
        }

        fun getRequestingLocation(context: Context): Boolean {
            return getPreferences(context).getBoolean(KEY_REQUESTING_LOCATION, false)
        }

        fun saveMapSetup(context: Context, mapId: String, mapSetup: MapSetupSnapshot) {
        val json = mapSetup.toJson()
            setJsonProperty(context, mapId, json)
        }

        fun getMapSetup(context: Context, mapId: String): MapSetupSnapshot {
            val json = getJsonProperty(context, mapId, null)
            return if (json.isNullOrBlank()) {
                // Let's use AccuTerra Vector as the default map
                MapSetupSnapshot(AccuTerraStyle.VECTOR)
            } else {
                json.jsonToClass(MapSetupSnapshot::class.java)
            }
        }

        /* * * * * * * * * * * * */
        /*        PRIVATE        */
        /* * * * * * * * * * * * */

        private fun getPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        }

        private fun setJsonProperty(context: Context, key: String, value: String) {
            getPreferences(context)
                .edit()
                .putString(key, value)
                .apply()
        }

        private fun getJsonProperty(context: Context, key: String, default: String? = null) : String? {
            return getPreferences(context)
                    .getString(key, default)
        }

    }

}