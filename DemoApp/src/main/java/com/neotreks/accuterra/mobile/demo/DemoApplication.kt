package com.neotreks.accuterra.mobile.demo

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.mapbox.mapboxsdk.Mapbox
import com.neotreks.accuterra.mobile.sdk.SdkManager

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

        Mapbox.getInstance(this, BuildConfig.MAPBOX_TOKEN)

        // When set to true the Mapbox SDK will append SKU to each offline download request
        // This way the offline request is charged per user session
        Mapbox.UseSKUForOfflineDownload = true

        //
        // It is recommended to initialize the SDK context at the very beginning of the APK start.
        //
        // Please note that it is still needed to call at least once after application installation
        // the [SdkManager.initSdk(Context, IAccessProvider, SdkConfig)] to configure the SDK.
        // After that initialization just with the context is sufficient.
        SdkManager.initSdkContext(applicationContext)

    }
}