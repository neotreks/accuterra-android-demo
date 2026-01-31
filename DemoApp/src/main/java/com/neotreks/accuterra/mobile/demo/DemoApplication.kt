package com.neotreks.accuterra.mobile.demo

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.neotreks.accuterra.mobile.sdk.SdkManager
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap

@Suppress("unused")
class DemoApplication: Application() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    val onlineTripVMStore: ViewModelStore by lazy {
        ViewModelStore()
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate() {
        super.onCreate()

        MapLibre.getInstance(this)
        //
        // It is recommended to initialize the SDK context at the very beginning of the APK start.
        //
        // Please note that it is still needed to call at least once after application installation
        // the [SdkManager.initSdk(Context, IAccessProvider, SdkConfig)] to configure the SDK.
        // After that initialization just with the context is sufficient.
        SdkManager.initSdkContext(applicationContext)
    }
}