package com.neotreks.accuterra.mobile.demo.offlinemap

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.location.engine.*
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.neotreks.accuterra.mobile.demo.DemoAppPreferences
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityNewOfflineMapBinding
import com.neotreks.accuterra.mobile.demo.extensions.bytesToHumanReadable
import com.neotreks.accuterra.mobile.demo.offline.ApkOfflineCacheBackgroundService
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.demo.util.*
import com.neotreks.accuterra.mobile.sdk.cache.OfflineCacheBackgroundService
import com.neotreks.accuterra.mobile.sdk.map.*
import com.neotreks.accuterra.mobile.sdk.map.cache.*
import com.neotreks.accuterra.mobile.sdk.trail.extension.toLatLngBounds
import com.neotreks.accuterra.mobile.sdk.trail.model.*
import com.neotreks.accuterra.mobile.sdk.util.LocationPermissionUtil
import java.lang.ref.WeakReference

class NewOfflineMapActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "NewOfflineMapActivity"
        private const val SHOW_MY_LOCATION_PERMISSIONS_REQUEST = 1
        private const val MAP_ID = "NewOfflineMap"
        private const val MAX_DOWNLOAD_SIZE_BYTES: Long = 1024 * 1024 * 1024 // 1 GB
        private const val KEY_OFFLINE_MAP_ID = "KEY_OFFLINE_MAP_ID"

        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, NewOfflineMapActivity::class.java)
        }

        fun createNavigateToIntent(context: Context, offlineMapId: String): Intent {
            return Intent(context, NewOfflineMapActivity::class.java)
                .apply {
                    putExtra(KEY_OFFLINE_MAP_ID, offlineMapId)
                }
        }
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: ActivityNewOfflineMapBinding

    private lateinit var accuTerraMapView: AccuTerraMapView
    private lateinit var mapboxMap: MapboxMap

    private var editOfflineMapId: String? = null

    // Location engine
    private var locationEngine: LocationEngine? = null

    // Current tracking option
    private var trackingOption = TrackingOption.NONE

    // Available tracking options
    private var trackingOptions = arrayOf(TrackingOption.NONE, TrackingOption.LOCATION)

    private val accuTerraMapViewListener = AccuTerraMapViewListener(this)

    private val currentLocationEngineListener = CurrentLocationEngineListener(this)

    private val networkStateReceiverListener = CurrentNetworkStateListener(this)

    // Current style id
    private var styleId: Int = 0

    private var offlineCacheBgService: OfflineCacheBackgroundService? = null

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
            Log.d(TAG, "offlineCacheService disconnected")
        }
    }

    private val cacheProgressListener = CacheProgressListener(this)

    private val offlineCacheServiceConnection = OfflineCacheBackgroundService.createServiceConnection(
        connectionListener
    )

    private lateinit var networkStateReceiver: NetworkStateReceiver

    // Available online styles
    private val styles: List<String> = listOf(
        AccuTerraStyle.VECTOR,
        HereMapsStyle.SATELLITE
    )

    // Available offline styles
    private val offlineStyles: List<String> = listOf(
        AccuTerraStyle.VECTOR,
        HereMapsStyle.SATELLITE
    )

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parseIntent()

        networkStateReceiver = NetworkStateReceiver(this)

        binding = ActivityNewOfflineMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupToolbar()

        setupButtons()
        setupMap(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        accuTerraMapView.onResume()

        networkStateReceiver.addListener(networkStateReceiverListener)
        networkStateReceiver.onResume()
    }

    override fun onPause() {
        if (accuTerraMapView.isStyleLoaded()) {
            saveMapSetup(accuTerraMapView)
        }
        super.onPause()
        accuTerraMapView.onPause()

        try {
            networkStateReceiver.removeListener(networkStateReceiverListener)
            networkStateReceiver.onPause()
        } catch (ex: IllegalArgumentException) {
            Log.e(TAG, ex.message ?: "Error while unregistering network state listener")
            CrashSupport.reportError(ex)
        }
    }

    override fun onStart() {
        super.onStart()
        ApkOfflineCacheBackgroundService.bindService(this@NewOfflineMapActivity, lifecycleScope, offlineCacheServiceConnection)
        setButtonsEnable(false)
        accuTerraMapView.onStart()
    }

    override fun onStop() {
        Log.i(TAG, "onStop()")
        // this doesn't stop the service, because we called "startService" explicitly
        ApkOfflineCacheBackgroundService.unbindService(this, offlineCacheServiceConnection)
        super.onStop()
        accuTerraMapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        accuTerraMapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyLocationEngine()
        accuTerraMapView.removeListener(accuTerraMapViewListener)
        accuTerraMapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        accuTerraMapView.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SHOW_MY_LOCATION_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() &&
                    grantResults.all { result -> result ==  PackageManager.PERMISSION_GRANTED}) {
                    accuTerraMapView.setTracking(trackingOption, null)
                    initializeLocationEngine()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun parseIntent() {
        // Parse OfflineMapId
        if (intent.hasExtra(KEY_OFFLINE_MAP_ID)) {
            this.editOfflineMapId = intent.getStringExtra(KEY_OFFLINE_MAP_ID)
        }
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        accuTerraMapView = binding.acitivityNewOfflineMapAccuterraMapView
        accuTerraMapView.onCreate(savedInstanceState)
        accuTerraMapView.addListener(accuTerraMapViewListener)

        // Get previous map setup if available
        var mapSetup = getSavedMapSetup()

        val networkAvailable = networkStateReceiver.isConnected()
        if (!networkAvailable) {
            styleId = styles.indexOfFirst { it == offlineStyles.first() }
        } else {
            // Try to find the style index for saved style
            val id = styles.indexOf(mapSetup.mapStyle)
            if (id != -1) {
                // Style found
                styleId = id
            }
        }
        // Adjust the map style because it might have changed above
        mapSetup = mapSetup.copy(mapStyle = styles[styleId])
        // Set the tracking option and update the button
        trackingOption = mapSetup.trackingOption ?: TrackingOption.NONE
        updateLocationTrackingButton()
        // Start map initialization
        accuTerraMapView.initialize(lifecycleScope, mapSetup)
    }

    /**
     * Utility method that saves current map setup like:
     * - map bounds
     * - base map layer
     * - location tracking mode
     */
    private fun saveMapSetup(accuTerraMapView: AccuTerraMapView) {
        val setup =  accuTerraMapView.getMapSetupSnapshot()
        DemoAppPreferences.saveMapSetup(this, MAP_ID, setup)
    }

    /**
     * Loads previously saved map setup/configuration
     */
    private fun getSavedMapSetup() : MapSetupSnapshot {
        return DemoAppPreferences.getMapSetup(this, MAP_ID)
    }

    private fun determineNextTrackingOption(): TrackingOption {
        return when (trackingOption) {
            TrackingOption.NONE_WITH_LOCATION -> TrackingOption.NONE_WITH_LOCATION
            TrackingOption.NONE -> TrackingOption.NONE
            else ->
                if (DemoAppPreferences.getRequestingLocation(this)) TrackingOption.NONE_WITH_LOCATION
                else TrackingOption.NONE
        }
    }

    private fun refreshEstimate() {
        lifecycleScope.launchWhenCreated {
            offlineCacheBgService?.offlineMapManager?.let {
                val latLngBounds = mapboxMap.projection.visibleRegion.latLngBounds
                val mapBounds = MapBounds(latLngBounds.latSouth, latLngBounds.lonWest, latLngBounds.latNorth, latLngBounds.lonEast)
                val includeImagery = binding.activityNewOfflineMapImageryToggle.isChecked
                val size = it.estimateAreaCacheSize(mapBounds, includeImagery)
                binding.activityNewOfflineMapEstimateText.text = getString(R.string.estimated_download_size, size.totalSize.bytesToHumanReadable())
            }
        }
    }

    private fun onAccuTerraMapViewReady() {

        setButtonsEnable(true)

        mapboxMap.addOnCameraIdleListener {
            refreshEstimate()
        }

        lifecycleScope.launchWhenResumed {
            // Edit mode, set text and zoom
            editOfflineMapId?.let { offlineMapId ->
                offlineCacheBgService?.offlineMapManager?.let { offlineMapManager ->
                    (offlineMapManager.getOfflineMap(offlineMapId) as? IAreaOfflineMap)?.let {
                        binding.activityNewOfflineMapNameEditText.setText(it.areaName)

                        binding.activityNewOfflineMapImageryToggle.isChecked = it.containsImagery
                        mapboxMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(it.bounds.toLatLngBounds(),
                                0.0,0.0,0)
                        )
                    }
                }
            }
        }

        mapboxMap.addOnMoveListener(object : MapboxMap.OnMoveListener {
            override fun onMoveBegin(moveGestureDetector: MoveGestureDetector) {}
            override fun onMove(moveGestureDetector: MoveGestureDetector) {
                setLocationTracking(determineNextTrackingOption())
            }

            override fun onMoveEnd(moveGestureDetector: MoveGestureDetector) {
            }
        })

        // Add layers and register map click handler
        lifecycleScope.launchWhenCreated {
            // Check if we need start the location tracking
            if (trackingOption.isShowLocationOption()) {
                tryInitializeLocationTracking()
            }

            refreshEstimate()
        }

        binding.activityNewOfflineMapMyLocationButton.show()
        binding.activityNewOfflineMapLayerButton.show()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.activityNewOfflineMapToolbar.generalToolbar)
        binding.activityNewOfflineMapToolbar.generalToolbarTitle.text = getString(R.string.activity_new_offline_map_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun setupButtons() {
        binding.activityNewOfflineMapMyLocationButton.setOnClickListener {
            cycleTrackingOption()
        }

        binding.activityNewOfflineMapLayerButton.setOnClickListener {
            cycleStyle()
        }

        binding.activityNewOfflineMapDownloadButton.setOnClickListener {
            setButtonsEnable(false)
            lifecycleScope.launchWhenCreated {
                offlineCacheBgService?.offlineMapManager?.let { offlineMapManager ->
                    val latLngBounds = mapboxMap.projection.visibleRegion.latLngBounds
                    val mapBounds = MapBounds(latLngBounds.latSouth, latLngBounds.lonWest,
                        latLngBounds.latNorth, latLngBounds.lonEast)
                    val includeImagery = binding.activityNewOfflineMapImageryToggle.isChecked
                    val size = offlineMapManager.estimateAreaCacheSize(mapBounds, includeImagery)

                    // Check max size
                    if (size.totalSize > MAX_DOWNLOAD_SIZE_BYTES) {
                        DialogUtil.buildOkDialog(this@NewOfflineMapActivity,
                            getString(R.string.activity_new_offline_map_map_too_big),
                            getString(R.string.activity_new_offline_map_max_map_size, MAX_DOWNLOAD_SIZE_BYTES.bytesToHumanReadable())).show()
                        setButtonsEnable(true)
                        return@launchWhenCreated
                    }

                    // Validate name
                    val areaName = binding.activityNewOfflineMapNameEditText.text.toString()
                    if (areaName.isBlank()) {
                        DialogUtil.buildOkDialog(this@NewOfflineMapActivity,
                            getString(R.string.activity_new_offline_map_missing_name),
                            "").show()
                        setButtonsEnable(true)
                        return@launchWhenCreated
                    }

                    // If this is edit, then delete the previous map
                    editOfflineMapId?.let {
                        offlineMapManager.deleteOfflineMap(it)
                    }

                    // Download
                    offlineMapManager.downloadAreaOfflineMap(mapBounds, areaName, includeImagery)

                    // Return to OfflineMaps activity
                    finish()
                } ?: return@launchWhenCreated
            }
        }

        binding.activityNewOfflineMapImageryToggle.setOnCheckedChangeListener { _, _ ->
            refreshEstimate()
        }
    }

    private fun setButtonsEnable(enabled: Boolean) {
        binding.activityNewOfflineMapDownloadButton.isClickable = enabled
    }

    private fun hasLocationPermissions(): Boolean {
        return LocationPermissionUtil.hasForegroundLocationPermissions(this)
    }

    private fun cycleStyle() {
        val isConnected = networkStateReceiver.isConnected()
        if (accuTerraMapView.isStyleLoaded()) {
            binding.activityNewOfflineMapLayerButton.isEnabled = false
            binding.activityNewOfflineMapMyLocationButton.isEnabled = false
            styleId += 1
            if (styleId == styles.count()) {
                styleId = 0
            }
            val style = styles[styleId]
            if (!isConnected && !offlineStyles.contains(style)) {
                // this style is not available offline, cycle to next
                cycleStyle()
                return
            }
            val styleProvider = ApkMapActivityUtil.getStyleProvider(style, this)
            accuTerraMapView.setStyle(style, styleProvider)
        }
    }

    private fun cycleTrackingOption() {
        when (trackingOption) {
            TrackingOption.NONE_WITH_LOCATION,
            TrackingOption.NONE_WITH_GPS_LOCATION -> setLocationTracking(TrackingOption.LOCATION)
            else -> {
                val nextTrackingOption = UiUtils.loopNextElement(trackingOption, trackingOptions)
                setLocationTracking(nextTrackingOption)
            }
        }
    }

    /**
     *  Set location tracking, changes icon of my location button.
     *  Triggers permission request if permission is required
     */
    @UiThread
    private fun setLocationTracking(trackingOption: TrackingOption) {

        if (this.trackingOption == trackingOption) {
            return
        }

        this.trackingOption = trackingOption
        updateLocationTrackingButton()

        if (hasLocationPermissions()) {
            accuTerraMapView.setTracking(trackingOption, null)
            if (trackingOption.isShowLocationOption()) {
                initializeLocationEngine()
            }
        } else {
            if (trackingOption.isShowLocationOption()) {
                tryInitializeLocationTracking()
            }
        }
    }

    private fun updateLocationTrackingButton() {
        val icon = UiUtils.getLocationTrackingIcon(trackingOption, this)
        binding.activityNewOfflineMapMyLocationButton.setImageDrawable(icon)
    }

    @UiThread
    private fun getVisibleMapBounds(): MapBounds {
        val mapBounds = mapboxMap.projection.visibleRegion.latLngBounds

        val latSouth = mapBounds.latSouth
        val latNorth = mapBounds.latNorth
        val lonWest = mapBounds.lonWest
        val lonEast = mapBounds.lonEast

        return MapBounds(latSouth, lonWest, latNorth, lonEast)
    }

    @UiThread
    private fun getVisibleMapCenter(): MapLocation {
        val mapCenter = mapboxMap.projection.visibleRegion.latLngBounds.center

        return MapLocation(mapCenter.latitude, mapCenter.longitude)
    }

    @UiThread
    private fun tryInitializeLocationTracking() {
        if (hasLocationPermissions()) {
            initializeLocationEngine()
        } else {
            LocationPermissionUtil.checkForegroundLocationPermission(this,
                SHOW_MY_LOCATION_PERMISSIONS_REQUEST)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializeLocationEngine() {

        if (this.locationEngine != null) {
            return
        }

        if (!hasLocationPermissions()) {
            return
        }

        DemoAppPreferences.setRequestingLocation(this, false)
        destroyLocationEngine()

        val locationEngineRequest = LocationEngineRequest.Builder(750)
            .setFastestInterval(750)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setDisplacement(1.0F)
            .build()

        val locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        locationEngine.requestLocationUpdates(locationEngineRequest,
            currentLocationEngineListener, Looper.getMainLooper())

        DemoAppPreferences.setRequestingLocation(this, true)

        // Set last known location
        locationEngine.getLastLocation(currentLocationEngineListener)

        this.locationEngine = locationEngine
    }

    private fun destroyLocationEngine() {
        this.locationEngine?.removeLocationUpdates(currentLocationEngineListener)
        this.locationEngine = null
    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    private class CacheProgressListener(activity: NewOfflineMapActivity): ICacheProgressListener {

        val weakActivity = WeakReference(activity)

        override fun onComplete(offlineMap: IOfflineMap) {
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
                    DialogUtil.buildOkDialog(activity, activity.getString(R.string.download_failed),errorMessage).show()
                }
            }
        }

        override fun onProgressChanged(offlineMap: IOfflineMap) {
        }
    }

    private class CurrentLocationEngineListener(activity: NewOfflineMapActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val weakActivity = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            val activity = weakActivity.get() ?: return
            activity.accuTerraMapView.updateLocation(result.lastLocation)
        }

        override fun onFailure(exception: Exception) {
            val activity = weakActivity.get() ?: return

            DialogUtil.buildOkDialog(activity,"Failed to obtain location update",
                exception.message?:"Unknown Error While obtaining location update")
        }
    }

    private class CurrentNetworkStateListener(activity: NewOfflineMapActivity)
        : NetworkStateReceiver.NetworkStateReceiverListener {

        private val weakActivity= WeakReference(activity)

        override fun onNetworkAvailable() {
            weakActivity.get()?.let {
                it.binding.activityNewOfflineMapLayerButton.isEnabled = true
            }
        }

        override fun onNetworkUnavailable() {
            weakActivity.get()?.let {
                val offlineCacheManager = it.offlineCacheBgService?.offlineMapManager ?: return
                it.lifecycleScope.launchWhenResumed {
                    if (offlineCacheManager.getOverlayOfflineMap()?.status == OfflineMapStatus.COMPLETE) {
                        if (it.accuTerraMapView.isStyleLoaded()) {
                            val currentStyle = it.styles[it.styleId]
                            if (!it.offlineStyles.contains(currentStyle)) {
                                it.cycleStyle()
                            }
                        }
                    } else {
                        it.binding.activityNewOfflineMapLayerButton.isEnabled = false
                    }
                }
            }
        }
    }

    private class AccuTerraMapViewListener(activity: NewOfflineMapActivity)
        : AccuTerraMapView.IAccuTerraMapViewListener {

        private val weakActivity= WeakReference(activity)

        override fun onInitialized(map: AccuTerraMapView) {
            val activity = weakActivity.get()
                ?: return

            activity.mapboxMap = map.getMapboxMap()
            activity.onAccuTerraMapViewReady()
        }

        override fun onInitializationFailed(map: AccuTerraMapView, errorMessage: String?) {
            weakActivity.get()?.let { context ->
                DialogUtil.buildOkDialog(context,"Error",
                    errorMessage?:"Unknown Error While Loading Map")
                    .show()
            }
        }

        override fun onStyleChanged(map: AccuTerraMapView) {
            val activity = weakActivity.get()
                ?: return
            activity.binding.activityNewOfflineMapLayerButton.isEnabled = true
            activity.binding.activityNewOfflineMapMyLocationButton.isEnabled = true
        }

        override fun onStyleChangeFailed(map: AccuTerraMapView, errorMessage: String?) {
        }

        override fun onStyleChangeStart(map: AccuTerraMapView) {
        }

        override fun onSignificantMapBoundsChange() {
        }

        override fun onTrackingModeChanged(mode: TrackingOption) {
        }
    }
}
