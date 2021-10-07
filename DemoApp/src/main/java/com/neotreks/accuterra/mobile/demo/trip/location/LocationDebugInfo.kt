package com.neotreks.accuterra.mobile.demo.trip.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.location.Location
import android.os.BatteryManager
import com.google.gson.annotations.SerializedName
import com.neotreks.accuterra.mobile.demo.util.NetworkStateReceiver

/**
 * Location debugging info
 */
data class LocationDebugInfo(

    @SerializedName("thrNm")
    val threadName: String?,

    @SerializedName("ext")
    val extras: String?,

    @SerializedName("mock")
    val isMock: Boolean?,

    @SerializedName("elpRt")
    val elapsedRealtime: Long?,

    @SerializedName("btr")
    val battery: Int?,

    @SerializedName("onl")
    val isOnline: Boolean,

) {

    companion object {

        @SuppressLint("StaticFieldLeak")
        private lateinit var netWorkStateReceiver: NetworkStateReceiver

        private lateinit var batteryManager: BatteryManager

        fun build(context: Context, location: Location?) : LocationDebugInfo {
            val appContext = context.applicationContext
            return LocationDebugInfo(
                threadName = Thread.currentThread().name,
                extras = buildExtractInfo(location),
                isMock = location?.isFromMockProvider,
                elapsedRealtime = location?.elapsedRealtimeNanos,
                battery = getBatteryLevel(appContext),
                isOnline = getIsOnline(appContext),
            )
        }

        private fun getIsOnline(context: Context): Boolean {
            if (!Companion::netWorkStateReceiver.isInitialized) {
                netWorkStateReceiver = NetworkStateReceiver(context)
            }
            return netWorkStateReceiver.isConnected()
        }

        private fun getBatteryLevel(context: Context): Int {
            if (!Companion::batteryManager.isInitialized) {
                batteryManager = context.getSystemService(BATTERY_SERVICE) as BatteryManager
            }
            // Get the battery percentage and store it in a INT variable
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }

        private fun buildExtractInfo(location: Location?): String? {
            val extras = location?.extras ?: return null
            val builder = StringBuilder()
            for (key in extras.keySet()) {
                builder.append("$key = ${extras[key]}")
                builder.append(" | ")
            }
            return builder.toString()
        }

    }

}
