package com.neotreks.accuterra.mobile.demo

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.mapboxsdk.location.OnLocationCameraTransitionListener
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import com.neotreks.accuterra.mobile.demo.offline.ApkOfflineCacheBackgroundService
import com.neotreks.accuterra.mobile.demo.settings.UserSettingsActivity
import com.neotreks.accuterra.mobile.demo.trip.location.LocationActivity
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.demo.util.ApkMapActivityUtil
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.demo.util.NetworkStateReceiver
import com.neotreks.accuterra.mobile.sdk.cache.OfflineCacheBackgroundService
import com.neotreks.accuterra.mobile.sdk.location.SdkLocationProvider
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraStyle
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.map.cache.*
import com.neotreks.accuterra.mobile.sdk.trail.model.MapPoint
import java.lang.ref.WeakReference

/**
 * Base activity for `driving screens` containing map with navigation or recording.
 */
abstract class BaseDrivingActivity : LocationActivity() {

    companion object {
        private const val TAG = "BaseDrivingActivity"
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var accuTerraMapView: AccuTerraMapView

    private var networkStateReceiver: NetworkStateReceiver? = null

    private val networkStateReceiverListener by lazy { CurrentNetworkStateListener(this) }

    private var offlineCacheBgService: OfflineCacheBackgroundService? = null

    // Location tracking mode
    private var currentTracking = TrackingOption.LOCATION
    private var lastGpsTracking: TrackingOption? = null

    // Available tracking options
    private val trackingOptions: Array<TrackingOption> =
        arrayOf(TrackingOption.LOCATION, TrackingOption.DRIVING)

    // Current style id
    private var currentStyle = AccuTerraStyle.VECTOR

    // Available online styles
    private val styles: Array<String> = arrayOf(
        AccuTerraStyle.VECTOR,
        Style.SATELLITE_STREETS
    )

    // Available offline styles
    private val offlineStyles: List<String> = listOf(
        Style.SATELLITE_STREETS,
        AccuTerraStyle.VECTOR
    )

    // Used to map
    private val connectionListener = object: OfflineCacheServiceConnectionListener {
        override fun onConnected(service: OfflineCacheBackgroundService) {
            offlineCacheBgService = service
            lifecycleScope.launchWhenCreated {
                offlineCacheBgService?.offlineMapManager?.addProgressListener(cacheProgressListener)
            }
        }
        override fun onDisconnected() {
            offlineCacheBgService?.offlineMapManager?.removeProgressListener(cacheProgressListener)
            offlineCacheBgService = null
        }
    }

    private val cacheProgressListener by lazy { CacheProgressListener(this) }

    private val offlineCacheServiceConnection = OfflineCacheBackgroundService.createServiceConnection(
        connectionListener
    )

    /* * * * * * * * * * * * */
    /*       ABSTRACT        */
    /* * * * * * * * * * * * */

    abstract fun getAccuTerraMapView(): AccuTerraMapView

    abstract fun getMapViewLoadingFailListener(): MapView.OnDidFailLoadingMapListener

    abstract fun getAccuTerraMapViewListener(): AccuTerraMapView.IAccuTerraMapViewListener

    protected abstract fun getDrivingModeButton(): FloatingActionButton

    protected abstract fun getMapLayerButton(): FloatingActionButton?

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkStateReceiver = NetworkStateReceiver(this)
    }

    override fun onResume() {
        Log.i(TAG, "onResume()")
        super.onResume()
        accuTerraMapView.onResume()

        networkStateReceiver?.addListener(networkStateReceiverListener)
        networkStateReceiver?.onResume()
    }

    override fun onPause() {
        Log.i(TAG, "onPause()")
        super.onPause()
        accuTerraMapView.onPause()
        networkStateReceiver?.onPause()
    }

    override fun onStart() {
        Log.i(TAG, "onStart()")
        super.onStart()
        accuTerraMapView.onStart()
        ApkOfflineCacheBackgroundService.bindService(this, lifecycleScope, offlineCacheServiceConnection)
    }

    override fun onStop() {
        Log.i(TAG, "onStop()")
        ApkOfflineCacheBackgroundService.unbindService(this, offlineCacheServiceConnection)
        super.onStop()
        accuTerraMapView.onStop()
    }

    override fun onLowMemory() {
        Log.w(TAG, "onLowMemory()")
        super.onLowMemory()
        accuTerraMapView.onLowMemory()
    }

    override fun onDestroy() {
        Log.i(TAG, "onLowMemory()")
        super.onDestroy()

        accuTerraMapView.removeListener(getAccuTerraMapViewListener())
        accuTerraMapView.removeOnDidFailLoadingMapListener(getMapViewLoadingFailListener())
        accuTerraMapView.onDestroy()

        getLocationService()?.removeLocationUpdates()
    }

    /* * * * * * * * * * * * */
    /*       PROTECTED       */
    /* * * * * * * * * * * * */

    protected open fun setupButtons() {
        getDrivingModeButton().setOnClickListener {
            onToggleLocationTrackingMode()
        }

        getMapLayerButton()?.setOnClickListener {
            onToggleMapStyle()
        }
    }

    protected open fun setupMap(savedInstanceState: Bundle?) {
        Log.d(TAG, "setupMap()")

        accuTerraMapView = getAccuTerraMapView()
        accuTerraMapView.onCreate(savedInstanceState)
        accuTerraMapView.addListener(getAccuTerraMapViewListener())
        accuTerraMapView.addOnDidFailLoadingMapListener(getMapViewLoadingFailListener())

        accuTerraMapView.initialize(lifecycleScope, currentStyle)
    }

    protected fun zoomToWayPoint(wayPoint: MapPoint) {
        Log.d(TAG, "zoomToWayPoint(wayPoint: ${wayPoint.id}")

        accuTerraMapView.zoomToPoint(wayPoint.location)
    }

    /**
     *  Set location tracking, changes icon of my location button.
     *  Triggers permission request if permission is required
     */
    @UiThread
    protected open fun setLocationTracking(trackingOption: TrackingOption) {
        currentTracking = trackingOption
        if (trackingOption.isTrackingOption()) {
            lastGpsTracking = trackingOption
        }
        val icon = UiUtils.getLocationTrackingIcon(trackingOption, this)
        getDrivingModeButton().setImageDrawable(icon)

        if (hasLocationPermissions()) {
            getAccuTerraMapView().setTracking(trackingOption, null)
        }
    }
    /** Avoids centering map to the current location */
    protected fun resetLocationCentering() {
        if (currentTracking == TrackingOption.LOCATION) {
            setLocationTracking(TrackingOption.NONE_WITH_LOCATION)
        }
        if (currentTracking == TrackingOption.DRIVING) {
            setLocationTracking(TrackingOption.NONE_WITH_DRIVING)
        }
    }

    protected fun switchToDrivingMode() {
        Log.i(TAG, "switchToDrivingMode()")

        // the driving mode requires Location permissions
        accuTerraMapView.setTracking(TrackingOption.DRIVING, null)
    }

    protected fun exitDrivingMode(transitionListener: OnLocationCameraTransitionListener?) {
        Log.i(TAG, "exitDrivingMode()")

        accuTerraMapView.setTracking(TrackingOption.NONE_WITH_GPS_LOCATION, transitionListener)
        getDrivingModeButton().visibility = View.VISIBLE
    }

    protected open fun updateLocationOnMap(location: Location?) {
        Log.v(TAG, "updateLocationOnMap(location=$location)")
        if (accuTerraMapView.isStyleLoaded()) {
            accuTerraMapView.updateLocation(location)
        }
    }

    protected fun initializeGpsLocationUpdates() {
        Log.d(TAG, "initializeGpsLocationUpdates()")

        if (hasLocationPermissions()) {
            val provider = getPreferredLocationProvider()
            getLocationService()?.requestLocationUpdates(provider)
        } else {
            requestPermissions()
        }
    }

    protected fun onToggleMapStyle() {
        val isConnected = networkStateReceiver?.isConnected() ?: true
        if (accuTerraMapView.isStyleLoaded()) {
            getMapLayerButton()?.isEnabled = false
            getDrivingModeButton().isEnabled = false
            currentStyle = UiUtils.loopNextElement(currentStyle, styles)
            if (!isConnected && !offlineStyles.contains(currentStyle)) {
                // this style is not available offline, cycle to next
                onToggleMapStyle()
                return
            }
            val styleProvider = ApkMapActivityUtil.getStyleProvider(currentStyle, this)
            accuTerraMapView.setStyle(currentStyle, styleProvider)
        }
    }

    protected open fun onToggleLocationTrackingMode() {
        val currentCameraMode = getAccuTerraMapView().getMapboxMap().locationComponent.cameraMode
        val isMapTracking = getAccuTerraMapView().isTrackingCameraMode(currentCameraMode)
        val lastGpsTracking = lastGpsTracking
        if (isMapTracking || lastGpsTracking == null) {
            val newTracking: TrackingOption = UiUtils.loopNextElement(
                currentTracking,
                this.trackingOptions
            )
            setLocationTracking(newTracking)
        } else {
            setLocationTracking(lastGpsTracking)
        }
    }

    protected open fun getPreferredLocationProvider(): SdkLocationProvider? {
        // Read from shared preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // We need to use default value also here since
        val providerString =  sharedPreferences.getString(UserSettingsActivity.KEY_LOCATION_PROVIDER, null)
        return if (providerString.isNullOrBlank()) {
            null
        } else {
            SdkLocationProvider.valueOf(providerString)
        }
    }

    // Location Activity Abstract Methods Implementation

    override fun getViewForSnackbar(): View {
        return getAccuTerraMapView()
    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    private class CurrentNetworkStateListener(activity: BaseDrivingActivity)
        : NetworkStateReceiver.NetworkStateReceiverListener {

        private val weakActivity= WeakReference(activity)

        override fun onNetworkAvailable() {
            weakActivity.get()?.let { activity ->
                activity.getMapLayerButton()?.isEnabled = true
            }
        }

        override fun onNetworkUnavailable() {
            weakActivity.get()?.let { activity ->
                val offlineCacheManager = activity.offlineCacheBgService?.offlineMapManager ?: return
                activity.lifecycleScope.launchWhenCreated {
                    if (offlineCacheManager.getOverlayOfflineMap()?.status == OfflineMapStatus.COMPLETE) {
                        if (activity.accuTerraMapView.isStyleLoaded()) {
                            val currentStyle = activity.currentStyle
                            if (!activity.offlineStyles.contains(currentStyle)) {
                                activity.onToggleMapStyle()
                            }
                        }
                    } else {
                        activity.getMapLayerButton()?.isEnabled = false
                    }
                }
            }
        }
    }

    private class CacheProgressListener(activity: BaseDrivingActivity): ICacheProgressListener {

        val weakActivity = WeakReference(activity)

        override fun onComplete(offlineMap: IOfflineMap) {
            // We do not wan to do anything
        }

        override fun onComplete() {
            // We do not wan to do anything
        }

        override fun onError(error: HashMap<IOfflineResource, String>, offlineMap: IOfflineMap) {
            weakActivity.get()?.let { activity ->
                activity.lifecycleScope.launchWhenCreated {
                    val errorMessage = error.map { e ->
                        "${e.key.getResourceTypeName()}: ${e.value}"
                    }.joinToString("\n")
                    DialogUtil.buildOkDialog(activity, activity.getString(R.string.download_failed), errorMessage)
                        .show()
                }
            }
        }

        override fun onProgressChanged(offlineMap: IOfflineMap) {
            // We do not wan to show anything
        }
    }

}