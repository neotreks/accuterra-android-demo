package com.neotreks.accuterra.mobile.demo

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.UiThread
import androidx.appcompat.app.ActionBar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.OnLocationCameraTransitionListener
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.demo.trail.WaypointListAdapter
import com.neotreks.accuterra.mobile.demo.trail.TrailPoiDetailActivity
import com.neotreks.accuterra.mobile.demo.trip.recorded.BaseTripRecordingActivity
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripRecordingButtonsPanel
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripRecordingStatsPanel
import com.neotreks.accuterra.mobile.demo.ui.OnListItemActionClickedListener
import com.neotreks.accuterra.mobile.demo.ui.OnListItemClickedListener
import com.neotreks.accuterra.mobile.demo.util.*
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.NextWayPoint
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.map.TrailLayersManager
import com.neotreks.accuterra.mobile.sdk.map.navigation.ITrailNavigator
import com.neotreks.accuterra.mobile.sdk.map.navigation.ITrailNavigatorListener
import com.neotreks.accuterra.mobile.sdk.map.navigation.TrailNavigator
import com.neotreks.accuterra.mobile.sdk.map.query.TrailPoisQueryBuilder
import com.neotreks.accuterra.mobile.sdk.trail.model.Trail
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailDrive
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailDriveWaypoint
import com.neotreks.accuterra.mobile.sdk.trail.service.TrailLoadFilter
import com.neotreks.accuterra.mobile.sdk.trip.recorder.ITripRecorder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripStatistics
import kotlinx.android.synthetic.main.activity_driving.*
import kotlinx.android.synthetic.main.component_trip_recording_buttons.*
import kotlinx.android.synthetic.main.component_trip_recording_stats.*
import kotlinx.android.synthetic.main.driving_activity_toolbar.*
import kotlinx.android.synthetic.main.poi_item.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import java.text.NumberFormat
import java.util.*

class DrivingActivity : BaseTripRecordingActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val TAG = "DrivingActivity"

        private const val KEY_TRAIL_ID = "KEY_TRAIL_ID"

        fun createNavigateToIntent(context: Context, trailId: Long): Intent {
            return Intent(context, DrivingActivity::class.java)
                .apply {
                    putExtra(KEY_TRAIL_ID, trailId)
                }
        }
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val id = Date().time

    private lateinit var viewModel: DrivingViewModel

    private lateinit var trailLayersManager: TrailLayersManager
    private var isTrailsLayerManagersLoaded = false

    private val accuTerraMapViewListener = AccuTerraMapViewListener(this)
    private val mapViewLoadingFailListener = MapLoadingFailListener(this)
    private val trailNavigatorListener = TrailNavigatorListener(this)

    private var trailNavigator: ITrailNavigator? = null
    private var trailPathLocationSimulator: TrailPathLocationSimulator? = null

    private var listSize = ListViewSize.MINIMUM
    private val distanceFormatter = Formatter.getDistanceFormatter()
    private lateinit var poiListAdapter: WaypointListAdapter

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving)

        viewModel = ViewModelProvider(this).get(DrivingViewModel::class.java)

        initRecordingPanel()
        setupButtons()
        setupListView()
        setupNextWayPointLabels()
        setupToolbar()

        parseIntent()
        registerTrailObserver()

        // Let's setup the map as the last component
        setupMap(savedInstanceState)
    }

    override fun onDestroy() {
        Log.i(TAG, "onLowMemory()")
        super.onDestroy()

        trailPathLocationSimulator?.destroy()

        trailNavigator?.removeListener(trailNavigatorListener)
    }

    override fun onBackPressed() {
        val hasActiveTrip = viewModel.getTripRecorder(this).hasActiveTripRecording()
        if (hasActiveTrip) {
            toast(getString(R.string.general_recording_cannot_exit_while_recording))
            return
        } else {
            super.onBackPressed()
        }
    }

    override fun onLocationUpdated(location: Location) {
        Log.i(TAG, "ILocationUpdateListener.onLocationUpdated()")
        Log.d(TAG, location.toString())

        viewModel.setLastLocation(location)
    }

    /*  IMPLEMENTATION OF THE ABSTRACT METHODS   */

    override fun getAccuTerraMapView(): AccuTerraMapView {
        return activity_driving_accuterra_map_view
    }

    override fun getAccuTerraMapViewListener(): AccuTerraMapView.IAccuTerraMapViewListener {
        return accuTerraMapViewListener
    }

    override fun getMapViewLoadingFailListener(): MapView.OnDidFailLoadingMapListener {
        return mapViewLoadingFailListener
    }

    override fun getDrivingModeButton(): FloatingActionButton {
        return activity_driving_to_driving_mode_button
    }

    override fun getMapLayerButton(): FloatingActionButton? {
        return activity_driving_layer_button
    }

    // Location Activity Abstract Methods Implementation

    override fun onLocationPermissionsGranted() {
        tryRegisteringLocationObservers()
    }

    override fun onLocationServiceBind() {
        if (BuildConfig.USE_TRAIL_PATH_EMULATOR) {
            initializeTrailPathSimulatorLocationEngine()
        } else {
            initializeGpsLocationUpdates()
        }
    }

    override fun getLastLocationLiveData(): MutableLiveData<Location?> {
        return viewModel.lastLocation
    }

    // BaseTripRecordingActivity Abstract Methods Implementation

    override fun getTripStatisticsLiveData(): MutableLiveData<TripStatistics?> {
        return viewModel.tripStatistics
    }

    override suspend fun getTripRecorder(): ITripRecorder {
        return viewModel.getTripRecorder(this)
    }

    override fun getTrailId(): Long? {
        val trailId = viewModel.trail.value?.info?.id
        checkNotNull(trailId) { "The trail ID must be available!" }
        return trailId
    }

    override fun getTripUuid(): String? {
        return viewModel.tripUUID
    }

    override fun onNewTripUuid(tripUuid: String?) {
        viewModel.tripUUID = tripUuid
    }


    override fun getSnackbarView(): View {
        return activity_driving_recording_panel
    }

    override fun getAddPoiButton(): View {
        return activity_driving_add_poi_button
    }

    override fun updateBackButton() {
        // TODO:boronmic IMPLEMENT!
    }

    override fun handleMapViewClick(latLng: LatLng): Boolean {
        val handled = searchTrailPois(latLng)
        if (handled) {
            return handled
        }
        return super.handleMapViewClick(latLng)
    }

    /* * * PRIVATE * * */

    private fun searchTrailPois(latLng: LatLng) : Boolean {
        val searchResult = TrailPoisQueryBuilder(trailLayersManager)
            .setCenter(latLng)
            .setTolerance(5.0f)
            .includeAllTrailLayers()
            .create()
            .execute()

        when(searchResult.trailPois.size) {
            0 -> {
                showNewSnackBar(null)
                trailLayersManager.highlightPOI(null)
                subSelectPoiById(null)
                return false
            }
            1 -> {
                val trailPois = searchResult.trailPois[0]
                when(trailPois.poiIds.size) {
                    0 -> throw IllegalStateException("Search returned a layer with an empty POIs set.")
                    1 -> {
                        handleTrailPoiMapClick(trailPois.poiIds.first())
                        return true
                    }
                    else -> {
                        handleMultiTrailPoiMapClick()
                        return true
                    }
                }
            }
            else -> {
                handleMultiTrailPoiMapClick()
                return true
            }
        }
    }

    private fun handleTrailPoiMapClick(poiId: Long) {
        loadAndShowPoiInfoAsync(poiId)
        trailLayersManager.highlightPOI(poiId)
    }

    private fun loadAndShowPoiInfoAsync(poiId: Long) {
        lifecycleScope.launchWhenCreated {
            // Unfold the list view and make sure the item is displayed
            setMediumListView() // Search related waypoint and display it in the list
            val waypoint = viewModel.trailDrive.waypoints.find { it.point.id == poiId }
                ?: return@launchWhenCreated
            subSelectPoiById(waypoint.id)
            val position = poiListAdapter.getPosition(waypoint)
            activity_driving_list.smoothScrollToPosition(position)
        }
    }
    private fun handleMultiTrailPoiMapClick() {
        val snackBar = Snackbar
            .make(getSnackbarView(), R.string.multiple_pois_in_this_area, Snackbar.LENGTH_SHORT)

        showNewSnackBar(snackBar)

        trailLayersManager.highlightPOI(null)
        subSelectPoiById(null)
    }

    private fun subSelectPoiById(id: Long?) {
        if (poiListAdapter.subSelectedItemId != id) {
            poiListAdapter.setSubSelectedId(id)
            poiListAdapter.notifyDataSetChanged()
        }
    }

    private fun registerTrailObserver() {
        Log.d(TAG, "registerTrailObserver()")

        viewModel.trail.observe(this, Observer { trail ->
            if (trail != null) {
                onTrailDataLoaded(trail, viewModel.trailDrive)
            }
        })
    }

    private fun onTrailDataLoaded(trail: Trail, trailDrive: TrailDrive) {
        Log.d(TAG, "onTrailDataLoaded()")
        poiListAdapter.addAll(trailDrive.waypoints)
        poiListAdapter.notifyDataSetChanged()

        activity_driving_toolbar_trail_name.text = trail.info.name
        activity_driving_toolbar_loading_panel.visibility = View.GONE
        activity_driving_toolbar_trail_name_panel.visibility = View.VISIBLE
        activity_driving_toolbar_way_point_panel.visibility = View.GONE

        viewModel.navigatorStatus.observe(this, Observer { navigatorStatus ->
            // do not update the list if the trail is lost or when going in the wrong direction
            if (navigatorStatus.status == NavigatorStatusType.NOT_READY
                || navigatorStatus.status == NavigatorStatusType.NAVIGATING) {
                updateNextWayPointInList(navigatorStatus.nextWayPoint)
            }
        })

        hideProgressBar()
    }

    private fun updateNextWayPointInList(situation: NextWayPoint?) {
        Log.d(TAG, "updateNextWayPointInList()")

        val poiId = situation?.trailPOI?.id
        if (poiListAdapter.selectedItemId != poiId) {
            poiListAdapter.selectedItemId = poiId
            // Refresh the list
            poiListAdapter.notifyDataSetChanged()
            // Update also the poi_item layout
            displayNextNextPoiInfo()
        }
    }

    private fun parseIntent() {
        // Parse + set the trail ID
        require(intent.hasExtra(KEY_TRAIL_ID)) { "No intent key $KEY_TRAIL_ID provided." }

        val trailId = intent.getLongExtra(KEY_TRAIL_ID, Long.MIN_VALUE)
        check(trailId != Long.MIN_VALUE) { "Invalid $KEY_TRAIL_ID value." }

        Log.i(TAG, "trailId=$trailId")
        try {
            viewModel.loadTrail(trailId, this)
        } catch (e: Exception) {
            longToast("Error while loading the trail: ${e.localizedMessage}")
            finish()
        }
    }

    private fun toggleListView() {
        Log.d(TAG, "toggleListView()")
        listSize = listSize.toggleMinMaxListView()

        moveListViewGuideLine()
        refreshList()
    }

    private fun setMediumListView() {
        if (listSize == ListViewSize.MEDIUM) {
            return
        }
        listSize = ListViewSize.MEDIUM
        moveListViewGuideLine()
        refreshList()
    }

    private fun refreshList() {
        val displayDescriptions = listSize != ListViewSize.MINIMUM
        poiListAdapter.setDisplayDescription(displayDescriptions)
        poiListAdapter.notifyDataSetChanged()
        displayNextNextPoiInfo()
    }

    private fun displayNextNextPoiInfo() {
        // Scroll to nex position if possible
        val selectedIndex = poiListAdapter.getSelectedItemIndex()
        val count = poiListAdapter.count
        if (selectedIndex == -1) {
            // Nothing is selected -> nothing to display
            poi_item_name.text = ""
            poi_item_millage.text = ""
            return
        }
        // Get the nextNext item but avoid out of index
        val nextNextItem = if (count > selectedIndex + 1) {
            poiListAdapter.getItem(selectedIndex + 1)
        } else {
            // Select the last one
            poiListAdapter.getItem(poiListAdapter.count - 1)
        }
        // Update the poi_item labels
        poi_item_name.text = nextNextItem?.point?.name
        nextNextItem?.distanceMarker?.let { distance ->
            poi_item_millage.text = Formatter.getDistanceFormatter().formatDistance(this, distance)
        }
    }

    private fun setupToolbar() {
        Log.v(TAG, "setupToolbar()")

        supportActionBar?.apply {
            displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
            setCustomView(R.layout.driving_activity_toolbar)
        } ?: throw IllegalStateException("Toolbar not found. Have you disabled it in manifest?")

        activity_driving_toolbar_loading_panel.visibility = View.VISIBLE
        activity_driving_toolbar_trail_name_panel.visibility = View.GONE
        activity_driving_toolbar_way_point_panel.visibility = View.GONE
    }

    override fun setupButtons() {
        // Setup recording buttons
        super.setupButtons()

        Log.v(TAG, "setupButtons()")

        activity_driving_poi_list_expander.drawDownArrowOnLeftSide()
        activity_driving_poi_list_expander.setOnClickListener {
            toggleListView()
        }

        activity_driving_layer_button.setOnClickListener {
            onToggleMapStyle()
        }

    }

    private fun initRecordingPanel() {
        buttonsPanel = TripRecordingButtonsPanel(component_trip_recording_stat_duration,
            component_trip_recording_stat_distance, component_trip_recording_start_button,
            component_trip_recording_stop_button, component_trip_recording_resume_button,
            component_trip_recording_finish_button, activity_driving_add_poi_button, this)

        statsPanel = TripRecordingStatsPanel(
            component_trip_recording_stats_speed, component_trip_recording_stats_heading,
            component_trip_recording_stats_elevation, component_trip_recording_stats_lat,
            component_trip_recording_stats_lon, this
        )
    }


    private fun setupListView() {

        poiListAdapter = WaypointListAdapter(this, mutableListOf(), displayDescription = false, displayAction = true)
        activity_driving_list.adapter = poiListAdapter

        makeListVisible(false)

        // Zoom to POI
        poiListAdapter.setOnListItemClickedListener(object : OnListItemClickedListener<TrailDriveWaypoint> {
            override fun onListItemClicked(item: TrailDriveWaypoint, view: View) {
                exitDrivingMode(object: OnLocationCameraTransitionListener {
                    override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                        onTrailPoiClicked(item)
                    }

                    override fun onLocationCameraTransitionCanceled(cameraMode: Int) {
                        onTrailPoiClicked(item)
                    }
                })
            }
        })
        // Display POI detail
        poiListAdapter.setOnListItemDetailClickedListener(
            object : OnListItemActionClickedListener<TrailDriveWaypoint> {
                override fun onListItemActionClicked(item: TrailDriveWaypoint) {
                    val intent = TrailPoiDetailActivity.createNavigateToIntent(this@DrivingActivity, item.id)
                    startActivity(intent)
                }
            })
    }

    private fun onTrailPoiClicked(item: TrailDriveWaypoint) {
        zoomToWayPoint(item.point)
        trailLayersManager.highlightPOI(item.point.id)
        subSelectPoiById(item.id)
        if (!isListMediumSize()) {
            setMediumListView()
            scrollListToItem(item)
        }
    }

    private fun scrollListToItem(point: TrailDriveWaypoint) {
        val position = poiListAdapter.getPosition(point)
        activity_driving_list.smoothScrollToPosition(position)
    }

    private fun isListMediumSize() = listSize == ListViewSize.MEDIUM

    private fun setupNextWayPointLabels() {
        Log.v(TAG, "setupNextWayPointLabels()")

        viewModel.navigatorStatus.observe(this, Observer { navigatorStatus ->
            @Suppress("REDUNDANT_ELSE_IN_WHEN")
            when(navigatorStatus.status) {
                NavigatorStatusType.NOT_READY -> {
                    // nothing to do
                }
                NavigatorStatusType.NAVIGATING -> showNextWayPointLabels(navigatorStatus.nextWayPoint!!)
                NavigatorStatusType.FINISHED -> showNavigationFinishedLabels(navigatorStatus.nextWayPoint!!)
                NavigatorStatusType.TRAIL_LOST -> showTrailLost()
                NavigatorStatusType.WRONG_DIRECTION -> showWrongDirection()
                else -> throw NotImplementedError("Unhandled NavigatorStatusType ${navigatorStatus.status}.")
            }
        })
    }

    private fun showNextWayPointLabels(situation: NextWayPoint) {
        Log.v(TAG, "updateNextWayPointLabels(situation=$situation)")

        val icon = when(situation.direction) {
            TrailNavigator.Direction.AT_POINT -> null
            else -> situation.direction
        }

        val distance = when(situation.direction) {
            TrailNavigator.Direction.AT_POINT -> null
            else -> situation.distance
        }

        showNextWayPointToolbar(icon, distance, situation.trailPOI.point.name ?: "End")
    }

    private fun showNavigationFinishedLabels(situation: NextWayPoint) {
        Log.v(TAG, "showNavigationFinishedLabels(situation=$situation)")

        require(situation.direction == TrailNavigator.Direction.AT_POINT)

        showNextWayPointToolbar(TrailNavigator.Direction.AT_POINT, null, situation.trailPOI.point.name ?: "End")
    }

    private fun showNextWayPointToolbar(icon: TrailNavigator.Direction?,
                                        distance: Float?,
                                        title: String) {

        if (icon == null) {
            activity_driving_toolbar_way_point_panel_direction_icon.visibility = View.GONE
        } else {
            activity_driving_toolbar_way_point_panel_direction_icon.visibility = View.VISIBLE
            val iconId = when(icon) {
                TrailNavigator.Direction.AT_POINT -> R.string.chequered_flag_character
                TrailNavigator.Direction.FORWARD -> R.string.upwards_arrow_character
                TrailNavigator.Direction.BACKWARD -> R.string.downwards_arrow_character
            }
            activity_driving_toolbar_way_point_panel_direction_icon.text = getString(iconId)
        }

        if (distance == null) {
            activity_driving_toolbar_way_point_distance.visibility = View.GONE
        } else {
            activity_driving_toolbar_way_point_distance.visibility = View.VISIBLE
            activity_driving_toolbar_way_point_distance.text =
                distanceFormatter.formatDistance(this, distance)
        }

        activity_driving_toolbar_way_point_name.text = title

        activity_driving_toolbar_trail_name_panel.visibility = View.GONE
        activity_driving_toolbar_way_point_panel.visibility = View.VISIBLE
        activity_driving_toolbar_wrong_direction.visibility = View.GONE
        activity_driving_toolbar_trail_lost.visibility = View.GONE
    }

    private fun showTrailLost() {
        Log.v(TAG, "showTrailLost()")

        activity_driving_toolbar_trail_name_panel.visibility = View.GONE
        activity_driving_toolbar_way_point_panel.visibility = View.GONE
        activity_driving_toolbar_wrong_direction.visibility = View.GONE
        activity_driving_toolbar_trail_lost.visibility = View.VISIBLE
    }

    private fun showWrongDirection() {
        Log.v(TAG, "showWrongDirection()")

        activity_driving_toolbar_trail_name_panel.visibility = View.GONE
        activity_driving_toolbar_way_point_panel.visibility = View.GONE
        activity_driving_toolbar_wrong_direction.visibility = View.VISIBLE
        activity_driving_toolbar_trail_lost.visibility = View.GONE
    }

    private fun moveListViewGuideLine() {
        Log.d(TAG, "moveListViewGuideLine(), bottomListSize=$listSize")

        when(listSize) {
            ListViewSize.MINIMUM -> {
                makeListVisible(false)
                //make only the activity_driving_main_view visible
                val guidelinePercent =
                        (activity_driving_poi_item.height.toFloat() / activity_driving_main_view.height)

                activity_driving_list_bottom_guideline.setGuidelinePercent(guidelinePercent)
                activity_driving_map_top_guideline.setGuidelinePercent(guidelinePercent)
                activity_driving_poi_list_expander.drawDownArrowOnLeftSide()
            }
            ListViewSize.MEDIUM -> {
                makeListVisible(true)
                activity_driving_list_bottom_guideline.setGuidelinePercent(0.4f)
                activity_driving_map_top_guideline.setGuidelinePercent(0.4f)
                activity_driving_poi_list_expander.drawUpArrowOnLeftSide()
            }
            ListViewSize.MAXIMUM -> {
                makeListVisible(true)
                val guidelinePercent = 1.0f -
                        (activity_driving_recording_panel.height.toFloat() / activity_driving_main_view.height)

                activity_driving_list_bottom_guideline.setGuidelinePercent(guidelinePercent)
                // keep map visible to avoid mapbox crashes caused by its height being 0
                // the list will overflow the map so it will look like it's hidden
                activity_driving_poi_list_expander.drawUpArrowOnLeftSide()
            }
        }
    }

    private fun makeListVisible(visible: Boolean) {
        if (visible) {
            activity_driving_list.emptyView = activity_driving_empty_list_label
        } else {
            // Need to remove the emptyView to make the `visibility = View.GONE` working
            activity_driving_list.emptyView = null
        }
        activity_driving_list.visibility = visible.visibility
        activity_driving_empty_list_label.visibility = visible.visibility
        activity_driving_poi_item.visibility = (!visible).visibility
    }

    @UiThread
    private fun hideProgressBar() {
        Log.d(TAG, "hideProgressBar()")
        activity_driving_list_loading_progress.visibility = View.GONE
    }

    private fun onAccuTerraMapViewReady() {
        Log.i(TAG, "onAccuTerraMapViewReady()")
        // Try to register location observers to obtain device location
        tryRegisteringLocationObservers()

        lifecycleScope.launchWhenCreated {
            addTrailLayers()
            setupRecordingAfterOnAccuTerraMapViewReady()
            getDrivingModeButton().show()
            // Add map move listener to update the icon
            getAccuTerraMapView().getMapboxMap().addOnMoveListener(object : MapboxMap.OnMoveListener {
                override fun onMoveBegin(moveGestureDetector: MoveGestureDetector) {}
                override fun onMoveEnd(moveGestureDetector: MoveGestureDetector) {}
                // Remove the [TrackOption.LOCATION] tracking
                override fun onMove(moveGestureDetector: MoveGestureDetector) {
                    resetLocationCentering()
                }
            })
        }
    }

    private suspend fun addTrailLayers() {
        Log.d(TAG, "addTrailLayers()")

        withContext(Dispatchers.Main) {
            trailLayersManager = getAccuTerraMapView().trailLayersManager
            isTrailsLayerManagersLoaded = true

            viewModel.trail.observe(this@DrivingActivity, Observer { trail ->
                if (trail == null) {
                    Log.d(TAG, "addTrailLayers() - the trail has not been loaded yet")
                } else {
                    // Load the one particular trail
                    lifecycleScope.launchWhenCreated {
                        val filter = TrailLoadFilter(listOf(trail.info.id))
                        trailLayersManager.addStandardLayers(filter)
                        filterTrail(trail)
                    }
                }
            })
        }
    }

    private fun filterTrail(trail: Trail) {
        Log.i(TAG, "filterTrail(trail=${trail.info.id})")

        lifecycleScope.launchWhenCreated {
            withContext(Dispatchers.Main) {
                trailLayersManager.setVisibleTrails(setOf(trail.info.id))
                trailLayersManager.highlightTrail(trail.info.id)

                trailLayersManager.showTrailPOIs(trail)

                viewModel.navigatorStatus.observe(this@DrivingActivity, Observer { navigatorStatus ->
                    if (navigatorStatus.status == NavigatorStatusType.NAVIGATING) {
                        trailLayersManager.highlightPOI(navigatorStatus.nextWayPoint!!.trailPOI.id)
                    }
                })
            }
        }
    }

    private fun tryRegisteringLocationObservers() {
        if (hasLocationPermissions()) {
            registerLocationObservers()
            registerStatisticsObserver()
            switchToDrivingMode()
        }
    }

    private fun registerLocationObservers() {
        viewModel.lastLocation.observe(this, Observer { lastLocation ->
            updateLocationOnMap(lastLocation)
        })

        viewModel.trail.observe(this, Observer { trail ->
            if (trail != null) {
                initializeTrailNavigator(trail, viewModel.trailDrive)
            }
        })
    }

    private fun initializeTrailNavigator(trail: Trail, trailDrive: TrailDrive) {
        Log.i(TAG, "initializeTrailNavigator(trail=${trail.info.id}")

        // In a production app the TrailNavigator reference should be held in a ViewModel.
        // We create it here any time the activity is restarted to show how to use the
        // nextExpectedWayPoint property.
        trailNavigator = ServiceFactory
            .getTrailNavigatorService(this)
            .getTrailNavigator(trailDrive)
            .apply {
                viewModel.nextExpectedWayPoint?.let { nextExpectedWayPoint ->
                    // restore the navigator
                    this.nextExpectedWayPoint = nextExpectedWayPoint
                }

                addListener(trailNavigatorListener)
            }

        viewModel.lastLocation.observe(this, Observer { lastLocation ->
            Log.v(TAG, "update trailNavigator location $lastLocation")

            if (lastLocation != null) {
                trailNavigator!!.evaluateLocation(lastLocation)
            }
        })
    }

    @UiThread
    private fun initializeTrailPathSimulatorLocationEngine() {
        viewModel.trail.observe(this, Observer { trail ->
            if (trail != null) {
                trailPathLocationSimulator = TrailPathLocationSimulator(viewModel.trailDrive, this)
                trailPathLocationSimulator?.start()
            }
        })
    }

    private fun updateNavigatorStatus(status: NavigatorStatus) {

        viewModel.navigatorStatus.value = status

        if (status.nextWayPoint != null) {
            val isNewWayPoint = viewModel.nextExpectedWayPoint != status.nextWayPoint.trailPOI
            viewModel.nextExpectedWayPoint = status.nextWayPoint.trailPOI

            if (isNewWayPoint && listSize != ListViewSize.MINIMUM) {
                val position = poiListAdapter.getPosition(status.nextWayPoint.trailPOI)
                activity_driving_list.smoothScrollToPosition(position)
            }
        }

    }

    private class AccuTerraMapViewListener(activity: DrivingActivity)
        : AccuTerraMapView.IAccuTerraMapViewListener {

        private val weakActivity= WeakReference(activity)

        override fun onInitialized(mapboxMap: MapboxMap) {
            Log.i(TAG, "AccuTerraMapViewListener.onInitialized()")

            val activity = weakActivity.get()
                ?: return

            activity.onAccuTerraMapViewReady()
        }

        override fun onStyleChanged(mapboxMap: MapboxMap) {
            Log.v(TAG, "AccuTerraMapViewListener.onStyleChanged()")
            weakActivity.get()?.let { activity ->
                activity.getMapLayerButton()?.isEnabled = true
                activity.getDrivingModeButton().isEnabled = true
            }
        }

        override fun onSignificantMapBoundsChange() {
            Log.v(TAG, "AccuTerraMapViewListener.onSignificantMapBoundsChange()")
            // nothing to do. only the GPS data matters - not the map bounds
        }

        override fun onTrackingModeChanged(mode: TrackingOption) {
            Log.i(TAG, "AccuTerraMapViewListener.onTrackingModeChanged(mode=$mode)")
            // TODO #16292 - anything we want to do here?
        }
    }

    private class MapLoadingFailListener(activity: DrivingActivity) :
        MapView.OnDidFailLoadingMapListener {

        private val weakActivity= WeakReference(activity)

        override fun onDidFailLoadingMap(errorMessage: String?) {
            Log.e(TAG, "MapLoadingFailListener.onDidFailLoadingMap(errorMessage=$errorMessage)")

            weakActivity.get()?.let { context ->
                DialogUtil.buildOkDialog(context,"Error",
                    errorMessage ?: "Unknown Error While Loading Map")
            }
        }
    }

    private class TrailNavigatorListener(activity: DrivingActivity)
        : ITrailNavigatorListener {

        private val weakActivity= WeakReference(activity)

        override fun onChange(location: Location, situation: NextWayPoint) {
            Log.i(TAG, "TrailNavigatorListener.onChange(situation=$situation)")

            if (BuildConfig.USE_TRAIL_NAVIGATOR_DEBUG_MODE) {
                saveDebugLine(location, "onChange", situation.trailPOI.pointIndexOnDrivePath)
            }

            weakActivity.get()
                ?.updateNavigatorStatus(NavigatorStatus.createNavigating(location, situation))
        }

        override fun onTrailEndReached(location: Location, situation: NextWayPoint) {
            Log.i(TAG, "TrailNavigatorListener.onTrailEndReached(situation=$situation)")

            if (BuildConfig.USE_TRAIL_NAVIGATOR_DEBUG_MODE) {
                saveDebugLine(location, "onTrailEndReached", situation.trailPOI.pointIndexOnDrivePath)
            }

            weakActivity.get()
                ?.updateNavigatorStatus(NavigatorStatus.createFinished(location, situation))
        }

        override fun onLocationIgnored(location: Location) {
            Log.w(TAG, "TrailNavigatorListener.onLocationIgnored()")

            if (BuildConfig.USE_TRAIL_NAVIGATOR_DEBUG_MODE) {
                saveDebugLine(location, "onLocationIgnored", null)
            }

            // nothing to do
        }

        override fun onTrailLost(location: Location) {
            Log.w(TAG, "TrailNavigatorListener.onTrailLost()")

            if (BuildConfig.USE_TRAIL_NAVIGATOR_DEBUG_MODE) {
                saveDebugLine(location, "onTrailLost", null)
            }

            weakActivity.get()
                ?.updateNavigatorStatus(NavigatorStatus.createTrailLost(location))
        }

        override fun onWrongDirection(location: Location) {
            Log.w(TAG, "TrailNavigatorListener.onWrongDirection()")

            if (BuildConfig.USE_TRAIL_NAVIGATOR_DEBUG_MODE) {
                saveDebugLine(location, "onWrongDirection", null)
            }

            weakActivity.get()
                ?.updateNavigatorStatus(NavigatorStatus.createWrongDirection(location))
        }

        private fun saveDebugLine(location: Location, resultType: String, navigationOrder: Int?) {
            val activity = weakActivity.get() ?: return

            val formatter = NumberFormat.getInstance(Locale.ROOT)

            OutputStreamWriter(
                FileOutputStream(File(activity.filesDir, "${activity.id}.csv"), true),
                Charset.forName("UTF-8")).use { writer ->
                writer
                    .append(formatter.format(location.latitude))
                    .append(",")
                    .append(formatter.format(location.longitude))
                    .append(",")
                    .append(formatter.format(location.time))
                    .append(",")
                    .append(resultType)
                    .append(",")
                    .append(navigationOrder?.toString() ?: "")
                    .appendLine()
            }

        }
    }
}
