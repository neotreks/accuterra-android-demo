package com.neotreks.accuterra.mobile.demo

import android.Manifest
import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.text.format.Formatter
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.core.location.*
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.neotreks.accuterra.mobile.demo.extensions.setOnSingleClickListener
import com.neotreks.accuterra.mobile.demo.trail.TrailListItem
import com.neotreks.accuterra.mobile.demo.trail.TrailListItemAdapter
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.demo.util.*
import com.neotreks.accuterra.mobile.demo.view.TrailFilterControl
import com.neotreks.accuterra.mobile.sdk.ProgressChangedListener
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.map.*
import com.neotreks.accuterra.mobile.sdk.map.cache.*
import com.neotreks.accuterra.mobile.sdk.map.query.TrailPoisQueryBuilder
import com.neotreks.accuterra.mobile.sdk.map.query.TrailsQueryBuilder
import com.neotreks.accuterra.mobile.sdk.trail.model.*
import kotlinx.android.synthetic.main.activity_trail_discovery.*
import kotlinx.android.synthetic.main.component_basic_tabs.*
import kotlinx.android.synthetic.main.progress_value_dialog_view.*
import kotlinx.android.synthetic.main.trail_filter_dialog.*
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

class TrailDiscoveryActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "TrailDiscoveryActivity"
        private const val SHOW_MY_LOCATION_PERMISSIONS_REQUEST = 1
        private const val MAP_ID = "TrailDiscoveryMap"

        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, TrailDiscoveryActivity::class.java)
        }
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private var bottomListSize = ListViewSize.MEDIUM

    private lateinit var trailsListAdapter: TrailListItemAdapter

    private lateinit var accuTerraMapView: AccuTerraMapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var trailLayersManager: TrailLayersManager
    private var isTrailsLayerManagersLoaded = false

    // Location engine
    private var locationEngine: LocationEngine? = null

    // Current tracking option
    private var trackingOption = TrackingOption.NONE

    // Available tracking options
    private var trackingOptions = arrayOf(TrackingOption.NONE, TrackingOption.LOCATION)

    private var zoomingToSearchedTrails = false

    private val accuTerraMapViewListener = AccuTerraMapViewListener(this)

    private val currentLocationEngineListener = CurrentLocationEngineListener(this)

    private val mapViewLoadingFailListener = MapLoadingFailListener(this)

    private val networkStateReceiverListener = CurrentNetworkStateListener(this)

    private val cacheProgressListener = CacheProgressListener(this)

    private lateinit var viewModel: TrailDiscoveryViewModel

    private var lastSnackbar: Snackbar? = null

    // Current style id
    private var styleId: Int = 0

    private var offlineMapService: OfflineMapService? = null

    private val connectionListener = object: OfflineMapServiceConnectionListener {
        override fun onConnected(service: OfflineMapService) {
            offlineMapService = service

            activity_trail_offlinemap_progress_layout.visibility = View.GONE

            if (networkStateReceiver?.isConnected() == true) {
                checkOverlayMapCache()
            }
        }

        override fun onDisconnected() {
            offlineMapService = null
            Log.e(TAG, "offlineMapService disconnected")
        }
    }

    private val offlineMapServiceConnection = OfflineMapService.createServiceConnection(
        connectionListener, cacheProgressListener
    )

    private lateinit var networkStateReceiver: NetworkStateReceiver

    // Available online styles
    private val styles: List<String> = listOf(
        AccuTerraStyle.VECTOR,
        Style.SATELLITE_STREETS
    )

    // Available offline styles
    private val offlineStyles: List<String> = listOf(
        AccuTerraStyle.VECTOR,
        Style.SATELLITE_STREETS
    )

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(Intent(this, OfflineMapService::class.java))
        networkStateReceiver = NetworkStateReceiver(this)

        viewModel = ViewModelProvider(this).get(TrailDiscoveryViewModel::class.java)

        setContentView(R.layout.activity_trail_discovery)
        setupToolbar()

        activity_trail_discovery_main_view.layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        setupTabs()
        setupButtons()
        setupList()
        setupMap(savedInstanceState)

        viewModel.trailsListItems.observe(this, Observer { items ->
            updateItemList(items)
        })

    }

    override fun onResume() {
        super.onResume()
        accuTerraMapView.onResume()

        networkStateReceiver.addListener(networkStateReceiverListener)
        networkStateReceiver.onResume()

        // Select the discovery tab if needed
        component_basic_tabs.getTabAt(AppBasicTabs.TAB_DISCOVER_INDEX)?.select()
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
        }
    }

    override fun onStart() {
        super.onStart()
        accuTerraMapView.onStart()
        Intent(this, OfflineMapService::class.java).also { intent ->
            bindService(intent, offlineMapServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        accuTerraMapView.onStop()
        // this doesn't stop the service, because we called "startService" explicitly
        unbindService(offlineMapServiceConnection)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        accuTerraMapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyLocationEngine()
        accuTerraMapView.removeListener(accuTerraMapViewListener)
        accuTerraMapView.removeOnDidFailLoadingMapListener(mapViewLoadingFailListener)
        accuTerraMapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        accuTerraMapView.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
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

    override fun onBackPressed() {
        if (activity_trail_discovery_toolbar_search_field.visibility == View.VISIBLE) {
            setTrailNameFilter(null, isGlobalSearch = false)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        if (menu != null) {
            menuInflater.inflate(R.menu.menu_trail_discovery, menu)
        }

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)

        if (menu != null) {
            val isShowingSearchField = activity_trail_discovery_toolbar_search_field.visibility == View.VISIBLE

            menu.findItem(R.id.trail_discovery_menu_item_search).isVisible  = !isShowingSearchField
            menu.findItem(R.id.trail_discovery_menu_item_cancel_search).isVisible  = isShowingSearchField
            menu.findItem(R.id.trail_discovery_menu_item_filter).isVisible  = !isShowingSearchField
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.trail_discovery_menu_item_search -> {
                showSearchUi()
                true
            }
            R.id.trail_discovery_menu_item_cancel_search -> {
                setTrailNameFilter(null, isGlobalSearch = false)
                true
            }
            R.id.trail_discovery_menu_item_filter -> {
                showFilterDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun setupMap(savedInstanceState: Bundle?) {
        accuTerraMapView = accuterra_map_view
        accuTerraMapView.onCreate(savedInstanceState)
        accuTerraMapView.addListener(accuTerraMapViewListener)
        accuTerraMapView.addOnDidFailLoadingMapListener(mapViewLoadingFailListener)

        // Get previous map setup if available
        var mapSetup = getSavedMapSetup()

        val networkAvailable = networkStateReceiver?.isConnected() ?: true
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

    private fun setupSearchEditText() {
        viewModel.defaultTitle = title
        viewModel.trailNameFilter = null // to keep this demo simple

        activity_trail_discovery_toolbar_search_field.text = viewModel.trailNameFilter
        activity_trail_discovery_toolbar_search_field.visibility = View.GONE

        activity_trail_discovery_toolbar_search_field.setOnEditorActionListener { view, actionId, _ ->

            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchString = view.text.toString().trim()

                setTrailNameFilter(searchString, isGlobalSearch = true)
            }

            // return false to indicate we did not consume the event
            // this way the keyboard will auto-hide on an <Enter> key press
            return@setOnEditorActionListener false
        }
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

    private fun onAccuTerraMapViewReady() {

        mapboxMap.addOnMoveListener(object : MapboxMap.OnMoveListener {
            override fun onMoveBegin(moveGestureDetector: MoveGestureDetector) {}
            override fun onMove(moveGestureDetector: MoveGestureDetector) {
                setLocationTracking(determineNextTrackingOption())
            }

            override fun onMoveEnd(moveGestureDetector: MoveGestureDetector) {}
        })

        // Add layers and register map click handler
        lifecycleScope.launchWhenCreated {
            addTrailLayers()
            registerMapClickHandler()
            // Check if we need start the location tracking
            if (trackingOption.isShowLocationOption()) {
                tryInitializeLocationTracking()
            }
            // Register trail observers
            registerTrailsObserverForMap()
        }

        activity_trail_discovery_my_location_button.show()
        activity_trail_discovery_layer_button.show()

        refreshTrailsListAsync(false, applyMapBoundsFilter = true)
    }

    private fun checkOverlayMapCache() {
        val offlineCacheManager = offlineMapService?.offlineMapManager ?: return
        lifecycleScope.launchWhenCreated {
            when(offlineCacheManager.getOfflineMapStatus(OfflineMapType.OVERLAY)) {
                OfflineMapStatus.NOT_CACHED, OfflineMapStatus.FAILED ->
                {
                    val estimateBytes = offlineCacheManager.estimateOverlayCacheSize()
                    val estimateText = Formatter.formatShortFileSize(
                        this@TrailDiscoveryActivity, estimateBytes)

                    // Display DIALOG
                    DialogUtil.buildYesNoDialog(
                        context = this@TrailDiscoveryActivity,
                        title = getString(R.string.download),
                        message = getString(R.string.download_overlay_prompt, estimateText),
                        code = {
                            lifecycleScope.launchWhenCreated {
                                offlineCacheManager.downloadOfflineMap(OfflineMapType.OVERLAY)
                            }
                        }
                    ).show()
                }
                else -> {
                    // Already in progress or complete
                }
            }
        }
    }

    private fun registerTrailsObserverForMap() {
        viewModel.trailsListItems.observe(this,
            Observer { trailsListItems ->
                val trailIds = trailsListItems
                    ?.map { trailListItem -> trailListItem.id }
                    ?.toSet()
                    ?: emptySet()
            })
    }

    private fun handleMapViewClick(latLng: LatLng) {
        searchTrailPois(latLng)
    }

    private fun searchTrailPois(latLng: LatLng) {
        val searchResult = TrailPoisQueryBuilder(trailLayersManager)
            .setCenter(latLng)
            .setTolerance(5.0f)
            .includeAllTrailLayers()
            .create()
            .execute()

        when(searchResult.trailPois.size) {
            0 -> {
                showNewSnackBar(null)
                searchTrails(latLng)
                trailLayersManager.highlightPOI(null)
            }
            1 -> {
                val trailPois = searchResult.trailPois[0]

                when(trailPois.poiIds.size) {
                    0 -> throw IllegalStateException("Search returned a layer with an empty POIs set.")
                    1 -> {
                        handleTrailPoiMapClick(trailPois.trailId, trailPois.poiIds.first())
                    }
                    else -> {
                        handleMultiTrailPoiMapClick()
                    }
                }
            }
            else -> {
                handleMultiTrailPoiMapClick()
            }
        }
    }

    private fun searchTrails(latLng: LatLng) {
        val searchResult = TrailsQueryBuilder(trailLayersManager)
            .setCenter(latLng)
            .setTolerance(5.0f)
            .includeAllTrailLayers()
            .create()
            .execute()

        when(searchResult.trailIds.size) {
            0 -> {
                selectTrail(null)
            }
            1 -> {
                handleTrailMapClick(searchResult.trailIds.first())
            }
            else -> {
                handleMultipleTrailsMapClick()
            }
        }
    }

    private fun handleMultipleTrailsMapClick() {
        val snackBar = Snackbar
            .make(activity_trail_discovery_main_view, R.string.multiple_trails_in_this_area, Snackbar.LENGTH_SHORT)

        showNewSnackBar(snackBar)

        selectTrail(null)
    }

    private fun handleTrailMapClick(trailId: Long) {
        selectTrail(trailId)

        if (bottomListSize.showListViewIcons) {
            scrollListToTrail(trailId)
        } else {
            navigateToTrailInfoScreen(trailId)
        }
    }

    private fun showNewSnackBar(snackBar: Snackbar?) {
        lastSnackbar?.dismiss()

        snackBar?.apply {
            anchorView = accuterra_map_view
            animationMode = Snackbar.ANIMATION_MODE_FADE
            show()
        }

        lastSnackbar = snackBar
    }

    private fun handleTrailPoiMapClick(trailId: Long, poiId: Long) {
        val snackBar = Snackbar
            .make(activity_trail_discovery_main_view, R.string.loading, Snackbar.LENGTH_INDEFINITE)

        showNewSnackBar(snackBar)

        loadAndShowPoiInfoAsync(trailId, poiId, snackBar)

        trailLayersManager.highlightPOI(poiId)
    }

    private fun loadAndShowPoiInfoAsync(trailId: Long, poiId: Long, snackBar: Snackbar) {
        val service = ServiceFactory.getTrailService(this@TrailDiscoveryActivity)

        lifecycleScope.launchWhenCreated {
            val trail = service.getTrailById(trailId)
                ?: throw IllegalArgumentException("Trail #$trailId not found.")

            val mapPoint = trail.navigationInfo.mapPoints
                .firstOrNull { mapPoint -> mapPoint.id == poiId }
                ?: throw IllegalArgumentException("No POI #$poiId found for trail #$trailId.")

            snackBar
                .setText(mapPoint.name ?: mapPoint.description ?: mapPoint.type.name)
                .setAction(R.string.close) {
                    trailLayersManager.highlightPOI(null)
                }
        }
    }

    private fun handleMultiTrailPoiMapClick() {
        val snackBar = Snackbar
            .make(activity_trail_discovery_main_view, R.string.multiple_pois_in_this_area, Snackbar.LENGTH_SHORT)

        showNewSnackBar(snackBar)

        trailLayersManager.highlightPOI(null)
    }

    private fun handleMapViewChanged() {
        if (zoomingToSearchedTrails) {
            /* The last time the map camera update was for the purpose
            * of showing search results. So end the search now and resume handling camera updates
            * with a refresh of trails in the area (normal map behavior)
            */

            zoomingToSearchedTrails = false
        } else {
            refreshTrailsListAsync(zoomToResult = false, applyMapBoundsFilter = true)
        }
    }

    private fun scrollListToTrail(trailId: Long) {
        val trailsListItems = viewModel.trailsListItems.value
            ?: return
        val listItem = trailsListItems
            .firstOrNull { item -> item.id == trailId }

        if (listItem != null) {
            val listIndex = trailsListItems.indexOf(listItem)

            activity_trail_discovery_list.setSelection(listIndex)
        } else {
            toast("Trail #$trailId is not on the current list.")
        }
    }

    private suspend fun addTrailLayers() {
        withContext(Dispatchers.Main) {
            trailLayersManager = accuTerraMapView.trailLayersManager
            isTrailsLayerManagersLoaded = true

            trailLayersManager.addStandardLayers()
        }
    }

    private fun registerMapClickHandler() {
        mapboxMap.addOnMapClickListener { latLng ->
            handleMapViewClick(latLng)
            return@addOnMapClickListener true
        }
    }

    private fun setupList() {

        trailsListAdapter = TrailListItemAdapter(this,
            listOf(),
            bottomListSize.showListViewIcons)

        trailsListAdapter.setOnTrailListItemIconClickedListener(object : TrailListItemAdapter.OnTrailListItemIconClickedListener {
            override fun onZoomToTrailIconClicked(trailListItem: TrailListItem) {
                selectTrail(trailListItem.id)
                zoomMapToTrail(trailListItem.id)
            }

            override fun onShowTrailInfoIconClicked(trailListItem: TrailListItem) {
                navigateToTrailInfoScreen(trailListItem.id)
            }
        })

        activity_trail_discovery_list.emptyView = activity_trail_discovery_empty_list_label
        activity_trail_discovery_list.adapter = trailsListAdapter

        activity_trail_discovery_list.setOnItemClickListener { _, _, position, _ ->
            val trailListItem = trailsListAdapter.getItem(position)
                ?: return@setOnItemClickListener
            handleTrailsListItemClick(trailListItem)
        }

        activity_trail_discovery_download_updates_button.setOnSingleClickListener(
            onClicked = { onDownloadUpdates() }
        )

        // hide the list at startup. make it look like it is loading
        // the actual load will start once the map becomes available
        displayProgressBar()
    }

    private fun onDownloadUpdates() {
        Log.d(TAG, "onDownloadUpdates")
        val dialog = DialogUtil.buildBlockingProgressDialog(this, getString(R.string.trail_updates_checking_updates))
        dialog.show()
        // Check if there is an internet connection
        val isConnected = networkStateReceiver.isConnected()
        if (!isConnected) {
            dialog.dismiss()
            toast(getString(R.string.general_error_no_internet_connection))
            return
        }
        // Check number of updates from the server
        lifecycleScope.launchWhenCreated {
            val service = ServiceFactory.getTrailService(this@TrailDiscoveryActivity)
            val hasUpdates = service.hasTrailDbUpdates()
            dialog.dismiss()
            if (hasUpdates.isFailure) {
                longToast(getString(R.string.trail_updates_checking_updates_failed, hasUpdates.errorMessage))
            } else {
                val updates = hasUpdates.value
                if (updates == null || updates.totalUpdates == 0) {
                    toast(getString(R.string.trail_updates_no_updates))
                } else {
                    // Start updating
                    startTrailUpdates()
                }
            }
        }
    }

    private fun startTrailUpdates() {
        Log.d(TAG, "startTrailUpdates")
        val dialog = DialogUtil.buildBlockingProgressValueDialog(this, getString(R.string.trail_updates_updating_trails))
        dialog.show()
        val service = ServiceFactory.getTrailService(this)
        val listener = object: ProgressChangedListener {
            override fun onProgressChanged(progress: Float) {
                lifecycleScope.launchWhenCreated {
                    dialog.progress_value_dialog_view_progress_bar.progress = (progress * 100).toInt()
                }
            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            val result = service.updateTrailDb(listener)
            // Close the dialog
            lifecycleScope.launchWhenCreated {
                // Lets refresh the map in case of changes
                if (result.value?.hasChangeActions() == true) {
                    accuTerraMapView.trailLayersManager.reloadLayers()
                    handleMapViewChanged()
                }
                if (result.isSuccess) {
                    if (result.value?.hasChangeActions() == true) {
                        dialog.dismiss()
                        longToast(getString(R.string.trail_updates_trail_db_updated,
                            (result.value?.changedTrailsCount()?.toString() ?: "-")))
                    } else {
                        dialog.dismiss()
                        toast(getString(R.string.trail_updates_trail_no_updates))
                    }
                } else {
                    dialog.dismiss()
                    longToast(getString(R.string.trail_updates_trail_db_update_failed, result.errorMessage))
                }
            }
        }
    }

    private fun updateItemList(items: List<TrailListItem>) {
        // Update items
        trailsListAdapter.setItems(items)
        // Display the list and hide the progress.
        hideProgressBar()
    }

    @SuppressLint("MissingPermission")
    private fun selectTrail(trailId: Long?) {
        viewModel.selectedTrailId = trailId
        trailsListAdapter.selectTrail(trailId)

        trailLayersManager.highlightTrail(trailId)

        if (hasLocationPermissions()) {
            locationEngine?.getLastLocation(currentLocationEngineListener)
            showTrailPOIsAsync(trailId)
        } else {
            showTrailPOIsAsync(trailId)
            tryInitializeLocationTracking()
        }
    }

    private fun showTrailPOIsAsync(trailId: Long?) {
        if (trailId == null) {
            trailLayersManager.hideAllTrailPOIs()
        } else {
            lifecycleScope.launchWhenCreated {
                val trail = loadTrail(trailId)

                if (trailId == viewModel.selectedTrailId) {
                    trailLayersManager.showTrailPOIs(trail)
                }
            }
        }
    }

    private fun handleTrailsListItemClick(trailListItem: TrailListItem) {
        selectTrail(trailListItem.id)

        if (!bottomListSize.showListViewIcons) {
            navigateToTrailInfoScreen(trailListItem.id)
        }
    }

    private fun navigateToTrailInfoScreen(trailId: Long) {
        val intent = TrailInfoActivity.createNavigateToIntent(this, trailId)
        startActivity(intent)
    }

    private fun zoomMapToTrail(trailId: Long) {
        lifecycleScope.launchWhenCreated {
            val fullTrail = loadTrail(trailId)

            setLocationTracking(TrackingOption.NONE)
            accuTerraMapView.zoomToBounds(fullTrail.locationInfo.mapBounds)
        }
    }

    private suspend fun loadTrail(trailId: Long): Trail {
        return ServiceFactory
            .getTrailService(this)
            .getTrailById(trailId)
            ?: throw IllegalArgumentException("The trail #$trailId not found.")
    }

    private fun zoomMapToTrails(trailIds: Iterable<Long>) {
        if (trailIds.none()) {
            return
        }

        lifecycleScope.launchWhenCreated {
            zoomingToSearchedTrails = true

            val mapBounds =
                ServiceFactory
                    .getTrailService(this@TrailDiscoveryActivity)
                    .getMapBoundsContainingTrails(trailIds)

            setLocationTracking(TrackingOption.NONE)

            accuTerraMapView.zoomToBounds(mapBounds)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(activity_trail_discovery_toolbar)

        setupSearchEditText()
    }

    private fun setupTabs() {
        component_basic_tabs.addOnTabSelectedListener(
            MainTabListener(object : MainTabListener.MainTabListenerHelper {
                override val context: Activity
                    get() = this@TrailDiscoveryActivity
                override fun getCurrentTabPosition(): Int {
                    return AppBasicTabs.TAB_DISCOVER_INDEX
                }
                override fun shouldFinish(): Boolean {
                    return false
                }
                override fun getCommunityFeedLocation(): MapLocation {
                    // Let's take the position from the middle of the map
                    val position = mapboxMap.cameraPosition.target
                    return MapLocation(
                        position.latitude,
                        position.longitude,
                        position.altitude
                    )
                }
            })
        )

        selectDefaultTabHack()
    }

    private fun setupButtons() {
        activity_trail_discovery_show_list_button.drawUpArrowOnLeftSide()
        activity_trail_discovery_show_list_button.setOnClickListener {
            toggleListView()
            trailsListAdapter.showActionIcons = bottomListSize.showListViewIcons
        }

        activity_trail_discovery_my_location_button.setOnClickListener {
            cycleTrackingOption()
        }

        activity_trail_discovery_layer_button.setOnClickListener {
            cycleStyle()
        }
    }

    private fun showSoftKeyboard() {
        val manager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as (InputMethodManager)
        manager.showSoftInput(activity_trail_discovery_toolbar_search_field, 0)
    }

    private fun hideSoftKeyboard() {
        val manager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as (InputMethodManager)
        manager.hideSoftInputFromWindow(activity_trail_discovery_toolbar_search_field.windowToken, 0)
    }

    private fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun cycleStyle() {
        val isConnected = networkStateReceiver?.isConnected() ?: true
        if (accuTerraMapView.isStyleLoaded()) {
            activity_trail_discovery_layer_button.isEnabled = false
            activity_trail_discovery_my_location_button.isEnabled = false
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
        activity_trail_discovery_my_location_button.setImageDrawable(icon)
    }

    private fun toggleListView() {
        bottomListSize = bottomListSize.toggleListView()

        moveListViewGuideLine()
    }

    private fun moveListViewGuideLine() {
        when(bottomListSize) {
            ListViewSize.MINIMUM -> {
                activity_trail_discovery_list_top_guideline.setGuidelinePercent(
                    //make only the activity_trail_discovery_main_view visible
                    1.0f - activity_trail_discovery_show_list_button.height.toFloat() / activity_trail_discovery_main_view.height.toFloat()
                )
                activity_trail_discovery_show_list_button.drawUpArrowOnLeftSide()
            }
            ListViewSize.MEDIUM -> {
                activity_trail_discovery_list_top_guideline.setGuidelinePercent(0.5f)
                activity_trail_discovery_show_list_button.drawUpArrowOnLeftSide()
            }
            ListViewSize.MAXIMUM -> {
                activity_trail_discovery_list_top_guideline.setGuidelinePercent(0.0f)
                activity_trail_discovery_show_list_button.drawDownArrowOnLeftSide()
            }
        }
    }

    private fun selectDefaultTabHack() {
        //TODO Honza: Replace this awful async call with something nice
        lifecycleScope.launchWhenCreated {
            delay(100)
            selectCurrentTab()
        }
    }

    private fun selectCurrentTab() {
        component_basic_tabs.getTabAt(AppBasicTabs.TAB_DISCOVER_INDEX)!!.select()
    }

    @UiThread
    private fun refreshTrailsListAsync(zoomToResult: Boolean, applyMapBoundsFilter: Boolean) {
        // Display progress bar
        displayProgressBar()

        val cameraZoomCallback = { trailBasicInfos: Iterable<TrailBasicInfo> ->
            if (zoomToResult) {
                val trailIds = trailBasicInfos.map { trailInfo -> trailInfo.id }
                zoomMapToTrails(trailIds)
            }
        }

        val trailNameFilter = viewModel.trailNameFilter
        val maxDifficultyLevel = viewModel.maxDifficultyLevel?.level
        val minUserRating = viewModel.minUserRating
        val maxTripDistance = viewModel.maxTripDistance
        val favorite = if (viewModel.favoriteOnly) true else null // We wan to use filter only in `favorite = true`

        val techRatingSearchCriteria = if (maxDifficultyLevel == null) {
            null
        } else {
            TechRatingSearchCriteria(maxDifficultyLevel, Comparison.LESS_EQUALS)
        }

        val userRatingSearchCriteria = if (minUserRating == null) {
            null
        } else {
            UserRatingSearchCriteria(minUserRating.toDouble(), Comparison.GREATER_EQUALS)
        }

        val lengthSearchCriteria = if (maxTripDistance == null) {
            null
        } else {
            LengthSearchCriteria(maxTripDistance.toDouble())
        }

        val limit = Int.MAX_VALUE

        if (applyMapBoundsFilter) {
            val mapBounds = if (bottomListSize == ListViewSize.MAXIMUM) {
                viewModel.lastSearchedMapBounds
            } else {
                getVisibleMapBounds()
            }

            val searchCriteria = TrailMapBoundsSearchCriteria(
                mapBounds,
                trailNameFilter,
                techRatingSearchCriteria,
                userRatingSearchCriteria,
                lengthSearchCriteria,
                favorite = favorite,
                limit = limit
            )

            // Run the query
            viewModel.findTrailsByMapBoundsAsync(searchCriteria, this, cameraZoomCallback)

            // backup the bounds settings. otherwise we won't be able to get them
            // if the map is hidden and user wants to filter it
            viewModel.lastSearchedMapBounds = mapBounds
        } else {
            val searchCriteria = TrailMapSearchCriteria(
                getVisibleMapCenter(),
                trailNameFilter,
                techRatingSearchCriteria,
                userRatingSearchCriteria,
                lengthSearchCriteria,
                favorite = favorite,
                limit = limit
            )

            // Run the query
            viewModel.findTrailsByMapCenterAsync(searchCriteria, this, cameraZoomCallback)
        }
    }

    @UiThread
    private fun displayProgressBar() {
        activity_trail_discovery_list_wrapper.visibility = View.GONE
        activity_trail_discovery_list_loading_progress.visibility = View.VISIBLE
    }

    @UiThread
    private fun hideProgressBar() {
        activity_trail_discovery_list_wrapper.visibility = View.VISIBLE
        activity_trail_discovery_list_loading_progress.visibility = View.GONE
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

    private fun showSearchUi() {
        activity_trail_discovery_toolbar_search_field.apply {
            setText(viewModel.trailNameFilter)
            visibility = View.VISIBLE
            requestFocus()
            selectAll()
        }

        showSoftKeyboard()

        invalidateOptionsMenu()
    }

    private fun setTrailNameFilter(trailNameFilter: String?, isGlobalSearch: Boolean) {
        viewModel.trailNameFilter = trailNameFilter

        if (isGlobalSearch) {
            viewModel.maxDifficultyLevel = null
            viewModel.minUserRating = null
            viewModel.maxTripDistance = null
            viewModel.favoriteOnly = false
        }

        title = if (viewModel.trailNameFilter.isNullOrBlank()) {
            viewModel.defaultTitle
        } else {
            viewModel.trailNameFilter
        }

        activity_trail_discovery_toolbar_search_field.visibility = View.GONE

        hideSoftKeyboard()
        invalidateOptionsMenu()

        refreshTrailsListAsync(isGlobalSearch, !isGlobalSearch)
    }

    private fun showFilterDialog() {
        TrailFilterDialog(this).show()
    }

    @UiThread
    private fun tryInitializeLocationTracking() {
        if (hasLocationPermissions()) {
            initializeLocationEngine()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                SHOW_MY_LOCATION_PERMISSIONS_REQUEST
            )
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

    private class CacheProgressListener(activity: TrailDiscoveryActivity):
        com.neotreks.accuterra.mobile.sdk.map.cache.CacheProgressListener {

        val weakActivity = WeakReference(activity)

        override fun onComplete(mapType: OfflineMapType, trailId: Long) {
            weakActivity.get()?.let {
                it.activity_trail_offlinemap_progress_layout.visibility = View.GONE
            }
        }

        override fun onError(error: HashMap<IOfflineResource, String>, mapType: OfflineMapType, trailId: Long) {
            weakActivity.get()?.let {
                val errorMessage = error.map { e ->
                    "${e.key.getResourceTypeName()}: ${e.value}"
                }.joinToString("\n")
                DialogUtil.buildOkDialog(it,it.getString(R.string.download_failed),errorMessage).show()
            }
        }

        override fun onProgressChanged(progress: Double, mapType: OfflineMapType, trailId: Long) {
            weakActivity.get()?.let {
                // Show what is downloading and progress
                val progressPercents = (100.0 * progress).toInt()
                it.activity_trail_offlinemap_progress_layout.visibility = View.VISIBLE
                it.activity_trail_offlinemap_progress_bar.progress = progressPercents

                var cacheName = "$mapType"
                if (mapType == OfflineMapType.TRAIL) {
                    cacheName += "($trailId)"
                }
                it.activity_trail_offlinemap_progress_label.text = it.getString(R.string.downloading_cache, cacheName, progressPercents)
            }
        }
    }

    private class CurrentLocationEngineListener(activity: TrailDiscoveryActivity) :
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

    private class CurrentNetworkStateListener(activity: TrailDiscoveryActivity)
        : NetworkStateReceiver.NetworkStateReceiverListener {

        private val weakActivity= WeakReference(activity)

        override fun onNetworkAvailable() {
            weakActivity.get()?.let {
                it.activity_trail_discovery_layer_button.isEnabled = true
            }
        }

        override fun onNetworkUnavailable() {
            weakActivity.get()?.let {
                val offlineCacheManager = it.offlineMapService?.offlineMapManager ?: return
                it.lifecycleScope.launchWhenCreated {
                    if (offlineCacheManager.getOfflineMapStatus(OfflineMapType.OVERLAY) == OfflineMapStatus.COMPLETE) {
                        if (it.accuTerraMapView.isStyleLoaded()) {
                            val currentStyle = it.styles[it.styleId]
                            if (!it.offlineStyles.contains(currentStyle)) {
                                it.cycleStyle()
                            }
                        }
                    } else {
                        it.activity_trail_discovery_layer_button.isEnabled = false
                    }
                }
            }
        }
    }

    private class AccuTerraMapViewListener(activity: TrailDiscoveryActivity)
        : AccuTerraMapView.IAccuTerraMapViewListener {

        private val weakActivity= WeakReference(activity)

        override fun onInitialized(mapboxMap: MapboxMap) {
            val activity = weakActivity.get()
                ?: return

            activity.mapboxMap = mapboxMap
            activity.onAccuTerraMapViewReady()
        }

        override fun onStyleChanged(mapboxMap: MapboxMap) {
            val activity = weakActivity.get()
                ?: return
            activity.activity_trail_discovery_layer_button.isEnabled = true
            activity.activity_trail_discovery_my_location_button.isEnabled = true
        }

        override fun onSignificantMapBoundsChange() {
            weakActivity.get()?.handleMapViewChanged()
        }

        override fun onTrackingModeChanged(mode: TrackingOption) {
            // TODO #16292 - anything we want to do here?
        }
    }

    private class MapLoadingFailListener(activity: TrailDiscoveryActivity) :
        MapView.OnDidFailLoadingMapListener {

        private val weakActivity= WeakReference(activity)

        override fun onDidFailLoadingMap(errorMessage: String?) {
            weakActivity.get()?.let { context ->
                DialogUtil.buildOkDialog(context,"Error", errorMessage?:"Unknown Error While Loading Map")
            }
        }
    }

    private class TrailFilterDialog(private val activity: TrailDiscoveryActivity)
        : Dialog(activity), TrailFilterControl.OnTrailFilterChangeListener {

        private var maxDifficultyLevel: TechnicalRating? = activity.viewModel.maxDifficultyLevel
        private var minUserRating: Int? = activity.viewModel.minUserRating
        private var maxTripDistance: Int? = activity.viewModel.maxTripDistance
        private var favoriteOnly: Boolean = activity.viewModel.favoriteOnly

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.trail_filter_dialog)

            trail_filter_dialog_filter.maxDifficultyLevel = maxDifficultyLevel
            trail_filter_dialog_filter.minUserRating = minUserRating
            trail_filter_dialog_filter.maxTripDistance = maxTripDistance
            trail_filter_dialog_filter.favoriteOnly = favoriteOnly

            trail_filter_dialog_done_button.setOnClickListener {
                applyFilter()
                dismiss()
            }
        }

        override fun onStart() {
            super.onStart()

            trail_filter_dialog_filter.setOnTrailFilterChangeListener(this)
        }

        override fun onStop() {
            super.onStop()
            trail_filter_dialog_filter.setOnTrailFilterChangeListener(null)
        }

        override fun onMaxDifficultyLevelChanged(
            trailFilterControl: TrailFilterControl,
            maxDifficultyLevel: TechnicalRating?
        ) {
            this.maxDifficultyLevel = maxDifficultyLevel
        }

        override fun onMinUserRatingChanged(
            trailFilterControl: TrailFilterControl,
            minUserRating: Int?
        ) {
            this.minUserRating = minUserRating
        }

        override fun onMaxTripDistanceChanged(
            trailFilterControl: TrailFilterControl,
            maxTripDistance: Int?
        ) {
            this.maxTripDistance = maxTripDistance
        }

        override fun onFavoriteChanged(trailFilterControl: TrailFilterControl, favoriteOnly: Boolean) {
            this.favoriteOnly = favoriteOnly
        }

        private fun applyFilter() {
            activity.viewModel.maxDifficultyLevel = maxDifficultyLevel
            activity.viewModel.minUserRating = minUserRating
            activity.viewModel.maxTripDistance = maxTripDistance
            activity.viewModel.favoriteOnly = favoriteOnly

            activity.refreshTrailsListAsync(false, applyMapBoundsFilter = true)
        }
    }
}
