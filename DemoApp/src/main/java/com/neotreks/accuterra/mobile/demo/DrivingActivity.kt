package com.neotreks.accuterra.mobile.demo

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mapbox.android.core.location.*
import com.mapbox.mapboxsdk.location.OnLocationCameraTransitionListener
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.demo.trail.OnListItemClickedListener
import com.neotreks.accuterra.mobile.demo.trail.PoiListAdapter
import com.neotreks.accuterra.mobile.demo.util.*
import com.neotreks.accuterra.mobile.sdk.map.*
import com.neotreks.accuterra.mobile.sdk.trail.model.MapPoint
import com.neotreks.accuterra.mobile.sdk.trail.model.Trail
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailPath
import kotlinx.android.synthetic.main.activity_driving.*
import kotlinx.android.synthetic.main.driving_activity_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class DrivingActivity : AppCompatActivity() {

    companion object {

        private const val TAG = "DrivingActivity"

        private const val KEY_TRAIL_ID = "KEY_TRAIL_ID"

        private const val SHOW_MY_LOCATION_PERMISSIONS_REQUEST = 1

        fun createNavigateToIntent(context: Context, trailId: Long): Intent {
            return Intent(context, DrivingActivity::class.java)
                .apply {
                    putExtra(KEY_TRAIL_ID, trailId)
                }
        }
    }

    private var requiredLocationPermissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

    private lateinit var viewModel: DrivingViewModel

    private lateinit var accuTerraMapView: AccuTerraMapView
    private lateinit var trailLayersManager: TrailLayersManager
    private var isTrailsLayerManagersLoaded = false

    private val accuTerraMapViewListener = AccuTerraMapViewListener(this)
    private val mapViewLoadingFailListener = MapLoadingFailListener(this)
    private val currentLocationEngineListener = CurrentLocationEngineListener(this)
    private val trailNavigatorListener = TrailNavigatorListener(this)

    private var locationEngine: LocationEngine? = null
    private var trailNavigator: TrailNavigator? = null
    private var trailPathLocationSimulator: TrailPathLocationSimulator? = null

    private var bottomListSize = ListViewSize.MEDIUM
    private val distanceFormatter = Formatter.getDistanceFormatter()
    private lateinit var poiListAdapter: PoiListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving)

        viewModel = ViewModelProvider(this).get(DrivingViewModel::class.java)
        parseIntent()

        setupToolbar()

        setupButtons()
        setupListView()
        setupNextWayPointLabels()
        setupMap(savedInstanceState)

        if (BuildConfig.USE_TRAIL_PATH_EMULATOR) {
            initializeTrailPathSimulatorLocationEngine()
        } else {
            initializeGpsLocationEngine()
        }

        registerTrailObserver()
    }

    override fun onResume() {
        Log.i(TAG, "onResume()")
        super.onResume()
        accuTerraMapView.onResume()
    }

    override fun onPause() {
        Log.i(TAG, "onPause()")
        super.onPause()
        accuTerraMapView.onPause()
    }

    override fun onStart() {
        Log.i(TAG, "onStart()")
        super.onStart()
        accuTerraMapView.onStart()
    }

    override fun onStop() {
        Log.i(TAG, "onStop()")
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

        accuTerraMapView.removeListener(accuTerraMapViewListener)
        accuTerraMapView.removeOnDidFailLoadingMapListener(mapViewLoadingFailListener)
        accuTerraMapView.onDestroy()

        trailPathLocationSimulator?.destroy()

        locationEngine?.removeLocationUpdates(currentLocationEngineListener)
        locationEngine = null
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        Log.i(TAG, "onRequestPermissionsResult(requestCode=$requestCode, permissions=${permissions.joinToString()}, grantResults=${grantResults.asList().joinToString()})")

        when (requestCode) {
            SHOW_MY_LOCATION_PERMISSIONS_REQUEST -> {
                if (grantResults.isNotEmpty() &&
                    grantResults.all { result -> result ==  PackageManager.PERMISSION_GRANTED}) {
                    tryRegisteringLocationObservers()
                } else {
                    Toast.makeText(this, "Cannot navigate.", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun registerTrailObserver() {
        Log.d(TAG, "registerTrailObserver()")

        viewModel.trail.observe(this, Observer { trail ->
            if (trail != null) {
                onTrailDataLoaded(trail)
            }
        })
    }

    private fun onTrailDataLoaded(trail: Trail) {
        Log.d(TAG, "onTrailDataLoaded()")

        viewModel.sortedWayPoints.clear()
        viewModel.sortWayPoints(null)
        viewModel.sortedWayPoints.addAll(

            trail.navigationInfo.waypoints
                .filter { point -> point.isWaypoint == true }
        )
        viewModel.sortWayPoints(TrailNavigator.Direction.FORWARD)
        poiListAdapter.notifyDataSetChanged()

        activity_driving_toolbar_trail_name.text = trail.info.name
        activity_driving_toolbar_loading_panel.visibility = View.GONE
        activity_driving_toolbar_trail_name_panel.visibility = View.VISIBLE
        activity_driving_toolbar_way_point_panel.visibility = View.GONE

        viewModel.trailNavigatorSituation.observe(this, Observer { situation ->
            updateNextWayPointInList(situation)
        })

        hideProgressBar()
    }

    private fun updateNextWayPointInList(situation: TrailNavigatorSituation?) {
        Log.d(TAG, "updateNextWayPointInList()")

        var hasDataSetChanged = viewModel.sortWayPoints(situation?.direction)

        val poiId = situation?.trailPOI?.id
        if (poiListAdapter.selectedItemId != poiId) {
            poiListAdapter.selectedItemId = poiId
            hasDataSetChanged = true
        }

        if (hasDataSetChanged) {
            poiListAdapter.notifyDataSetChanged()
        }
    }

    private fun parseIntent() {
        // Parse + set the trail ID
        require(intent.hasExtra(KEY_TRAIL_ID)) { "No intent key $KEY_TRAIL_ID provided." }

        val trailId = intent.getLongExtra(KEY_TRAIL_ID, Long.MIN_VALUE)
        check(trailId != Long.MIN_VALUE) { "Invalid $KEY_TRAIL_ID value." }

        Log.i(TAG, "trailId=$trailId")
        viewModel.loadTrail(trailId, this)
    }

    private fun toggleListView() {
        Log.d(TAG, "toggleListView()")
        bottomListSize = bottomListSize.toggleListView()

        moveListViewGuideLine()
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

    private fun setupButtons() {
        Log.v(TAG, "setupButtons()")

        activity_driving_show_list_button.drawUpArrowOnLeftSide()
        activity_driving_show_list_button.setOnClickListener {
            toggleListView()
        }

        activity_driving_to_driving_mode_button.setOnClickListener {
            // this button is hidden by default. it only becomes visible when the user
            // clicks a way point in the list.
            // in such case we expect that meanwhile the Location permission has been checked
            // and that the activity has finished if they were not granted
            switchToDrivingMode()
        }
    }

    private fun setupListView() {
        activity_driving_list.emptyView = activity_driving_empty_list_label

        poiListAdapter = PoiListAdapter(this, viewModel.sortedWayPoints)
        activity_driving_list.adapter = poiListAdapter

        poiListAdapter.setOnListItemClickedListener(object : OnListItemClickedListener<MapPoint> {
            override fun onListItemClicked(item: MapPoint) {
                exitDrivingMode(object: OnLocationCameraTransitionListener {
                    override fun onLocationCameraTransitionFinished(cameraMode: Int) {
                        zoomToWayPoint(item)
                    }

                    override fun onLocationCameraTransitionCanceled(cameraMode: Int) {
                        zoomToWayPoint(item)
                    }
                })
            }
        })
    }

    private fun zoomToWayPoint(wayPoint: MapPoint) {
        Log.d(TAG, "zoomToWayPoint(wayPoint: ${wayPoint.id}")

        accuTerraMapView.zoomToPoint(wayPoint.location)
    }

    private fun setupNextWayPointLabels() {
        Log.v(TAG, "setupNextWayPointLabels()")

        viewModel.trailNavigatorSituation.observe(this, Observer { situation ->
            updateNextWayPointLabels(situation)
        })
    }

    private fun updateNextWayPointLabels(situation: TrailNavigatorSituation?) {
        Log.v(TAG, "updateNextWayPointLabels(situation=$situation)")

        if (situation == null) {
            activity_driving_toolbar_loading_panel.visibility = View.GONE
            activity_driving_toolbar_trail_name_panel.visibility = View.VISIBLE
            activity_driving_toolbar_way_point_panel.visibility = View.GONE
        } else {
            activity_driving_toolbar_way_point_panel_arrow_forward.visibility =
                if (situation.direction == TrailNavigator.Direction.FORWARD) View.VISIBLE else View.GONE
            activity_driving_toolbar_way_point_panel_arrow_backward.visibility =
                if (situation.direction == TrailNavigator.Direction.BACKWARD) View.VISIBLE else View.GONE
            activity_driving_toolbar_way_point_name.text = situation.trailPOI.name
            activity_driving_toolbar_way_point_distance.text =
                distanceFormatter.formatDistance(this, situation.distance)

            activity_driving_toolbar_loading_panel.visibility = View.GONE
            activity_driving_toolbar_trail_name_panel.visibility = View.GONE
            activity_driving_toolbar_way_point_panel.visibility = View.VISIBLE
        }
    }

    private fun moveListViewGuideLine() {
        Log.d(TAG, "moveListViewGuideLine(), bottomListSize=$bottomListSize")

        when(bottomListSize) {
            ListViewSize.MINIMUM -> {
                //make only the activity_driving_main_view visible
                val guidelinePercent = 1.0f -
                        activity_driving_show_list_button.height.toFloat() / activity_driving_main_view.height.toFloat()

                activity_driving_list_top_guideline.setGuidelinePercent(guidelinePercent)
                activity_driving_map_bottom_guideline.setGuidelinePercent(guidelinePercent)
                activity_driving_show_list_button.drawUpArrowOnLeftSide()
            }
            ListViewSize.MEDIUM -> {
                activity_driving_list_top_guideline.setGuidelinePercent(0.5f)
                activity_driving_map_bottom_guideline.setGuidelinePercent(0.5f)
                activity_driving_show_list_button.drawUpArrowOnLeftSide()
            }
            ListViewSize.MAXIMUM -> {
                activity_driving_list_top_guideline.setGuidelinePercent(0.0f)
                // keep map visible to avoid mapbox crashes caused by its height being 0
                // the list will overflow the map so it will look like it's hidden
                activity_driving_map_bottom_guideline.setGuidelinePercent(0.5f)
                activity_driving_show_list_button.drawDownArrowOnLeftSide()
            }
        }
    }

    @UiThread
    private fun hideProgressBar() {
        Log.d(TAG, "hideProgressBar()")
        activity_driving_list_loading_progress.visibility = View.GONE
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        Log.d(TAG, "setupMap()")

        accuTerraMapView = activity_driving_accuterra_map_view
        accuTerraMapView.onCreate(savedInstanceState)
        accuTerraMapView.addListener(accuTerraMapViewListener)
        accuTerraMapView.addOnDidFailLoadingMapListener(mapViewLoadingFailListener)

        accuTerraMapView.initialize(AccuTerraStyle.VECTOR)
    }

    private fun onAccuTerraMapViewReady() {
        Log.i(TAG, "onAccuTerraMapViewReady()")
        tryRegisteringLocationObservers()

        lifecycleScope.launchWhenCreated {
            addTrailLayers()
        }
    }

    private suspend fun addTrailLayers() {
        Log.d(TAG, "addTrailLayers()")

        withContext(Dispatchers.Main) {
            trailLayersManager = accuTerraMapView.trailLayersManager
            isTrailsLayerManagersLoaded = true

            trailLayersManager.addStandardLayers()

            viewModel.trail.observe(this@DrivingActivity, Observer { trail ->
                if (trail == null) {
                    Log.d(TAG, "addTrailLayers() - the trail has not been loaded yet")
                } else {
                    filterTrail(trail)
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

                viewModel.trailNavigatorSituation.observe(this@DrivingActivity, Observer { situation ->
                    trailLayersManager.highlightPOI(situation?.trailPOI?.id)
                })
            }
        }
    }

    private fun tryRegisteringLocationObservers() {
        Log.d(TAG, "trySwitchingToDrivingMode()")

        if (hasLocationPermissions()) {
            registerLocationObservers()
            switchToDrivingMode()
        } else {
            Log.i(TAG, "trySwitchingToDrivingMode() - request permissions")
            ActivityCompat.requestPermissions(this, requiredLocationPermissions, SHOW_MY_LOCATION_PERMISSIONS_REQUEST)
        }
    }

    private fun switchToDrivingMode() {
        Log.i(TAG, "switchToDrivingMode()")

        // the driving mode requires Location permissions
        accuTerraMapView.setTracking(TrackingOption.DRIVING, null)
        activity_driving_to_driving_mode_button.visibility = View.GONE
    }

    private fun exitDrivingMode(transitionListener: OnLocationCameraTransitionListener?) {
        Log.i(TAG, "exitDrivingMode()")

        accuTerraMapView.setTracking(TrackingOption.NONE_WITH_GPS_LOCATION, transitionListener)
        activity_driving_to_driving_mode_button.visibility = View.VISIBLE
    }

    private fun registerLocationObservers() {
        viewModel.lastLocation.observe(this, Observer { lastLocation ->
            updateLocationOnMap(lastLocation)
        })

        viewModel.trail.observe(this, Observer { trail ->
            if (trail != null) {
                initializeTrailNavigator(trail, viewModel.trailPath)
            }
        })
    }

    private fun updateLocationOnMap(location: Location?) {
        Log.v(TAG, "updateLocationOnMap(location=$location)")

        accuTerraMapView.updateLocation(location)
    }

    private fun initializeTrailNavigator(trail: Trail, trailPath: TrailPath) {
        Log.i(TAG, "initializeTrailNavigator(trail=${trail.info.id}")

        trailNavigator = TrailNavigator(trail, trailPath)
            .apply {
                addListener(trailNavigatorListener)
            }

        viewModel.lastLocation.observe(this, Observer { lastLocation ->
            Log.v(TAG, "update trailNavigator location $lastLocation")

            trailNavigator!!.updateLocation(lastLocation)
        })
    }

    private fun initializeGpsLocationEngine() {
        Log.d(TAG, "initializeLocationEngine()")

        val locationEngineRequest = LocationEngineRequest.Builder(750)
            .setFastestInterval(750)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .build()

        locationEngine = LocationEngineProvider
            .getBestLocationEngine(this)
            .apply {
                requestLocationUpdates(locationEngineRequest, currentLocationEngineListener, Looper.getMainLooper())

                // Set last known location
                getLastLocation(currentLocationEngineListener)
            }
    }

    @UiThread
    private fun initializeTrailPathSimulatorLocationEngine() {
        viewModel.trail.observe(this, Observer { trail ->
            if (trail != null) {
                trailPathLocationSimulator = TrailPathLocationSimulator(viewModel.trailPath, currentLocationEngineListener)
                trailPathLocationSimulator?.start()
            }
        })
    }

    private fun hasLocationPermissions(): Boolean {
        Log.v(TAG, "hasLocationPermissions()")

        return requiredLocationPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
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
            // nothing to do?
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

    private class CurrentLocationEngineListener(activity: DrivingActivity) :
        LocationEngineCallback<LocationEngineResult> {

        private val weakActivity = WeakReference(activity)

        override fun onSuccess(result: LocationEngineResult) {
            Log.i(TAG, "CurrentLocationEngineListener.onSuccess()")
            Log.d(TAG, result.lastLocation?.toString() ?: "no location")

            val activity = weakActivity.get() ?: return
            activity.viewModel.lastLocation.value = result.lastLocation
        }

        override fun onFailure(exception: Exception) {
            Log.e(TAG, "CurrentLocationEngineListener.onFailure(exception=)", exception)

            val activity = weakActivity.get() ?: return

            DialogUtil.buildOkDialog(activity,"Failed to obtain location update",
                exception.message ?: "Unknown Error While obtaining location update")
        }
    }

    private class TrailNavigatorListener(activity: DrivingActivity)
        : TrailNavigator.ITrailNavigatorListener {

        private val weakActivity= WeakReference(activity)

        override fun onChange(situation: TrailNavigatorSituation) {
            Log.i(TAG, "TrailNavigatorListener.onChange(situation=$situation)")

            val activity = weakActivity.get() ?: return

            activity.viewModel.trailNavigatorSituation.value = situation
        }
    }
}
