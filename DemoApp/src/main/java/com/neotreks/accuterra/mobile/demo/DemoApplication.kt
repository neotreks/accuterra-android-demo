package com.neotreks.accuterra.mobile.demo

import android.app.Application
import com.mapbox.mapboxsdk.Mapbox

class DemoApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        Mapbox.getInstance(this, BuildConfig.MAPBOX_TOKEN)

    }
}