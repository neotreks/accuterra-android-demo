package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityTripRecordingBinding
import com.neotreks.accuterra.mobile.demo.databinding.ComponentTripRecordingButtonsBinding
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.demo.util.ActivityResult
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.sdk.SdkManager
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.model.ExtProperties
import com.neotreks.accuterra.mobile.sdk.trip.recorder.ITripRecorder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripStatistics
import java.lang.ref.WeakReference

class TripRecordingActivity : BaseTripRecordingActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: TripRecordingViewModel

    private val accuTerraMapViewListener = AccuTerraMapViewListener(this)
    private val mapViewLoadingFailListener = MapLoadingFailListener(this)

    private lateinit var binding: ActivityTripRecordingBinding

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val TAG = "RecordingActivity"

        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, TripRecordingActivity::class.java)
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(TripRecordingViewModel::class.java)

        initRecordingPanel()
        setupToolbar()
        setupButtons()

        // Let's setup the map as the last component
        setupMap(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menu?.let {
            menuInflater.inflate(R.menu.menu_trip_record, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.trip_record_menu_item_display_raw -> {
                SdkManager.debugConfig.tripConfiguration.displayRawLocations = !item.isChecked
                lifecycleScope.launchWhenResumed {
                    getAccuTerraMapView().tripLayersManager.reloadTripRecorderData()
                }
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.trip_record_menu_item_display_raw)!!
        item.isChecked = SdkManager.debugConfig.tripConfiguration.displayRawLocations
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onBackPressed() {
        val hasActiveTrip = viewModel.getTripRecorder(this).hasActiveTripRecording()
        if (hasActiveTrip) {
            toast(getString(R.string.general_recording_cannot_exit_while_recording))
            return
        }
        super.onBackPressed()
    }

    /* IMPLEMENTATION OF ABSTRACT METHODS */

    override fun getAccuTerraMapView(): AccuTerraMapView {
        return binding.activityTripRecordingAccuterraMapView
    }

    override fun getAccuTerraMapViewListener(): AccuTerraMapView.IAccuTerraMapViewListener {
        return accuTerraMapViewListener
    }

    override fun getMapViewLoadingFailListener(): MapView.OnDidFailLoadingMapListener {
        return mapViewLoadingFailListener
    }

    override fun getDrivingModeButton(): FloatingActionButton {
        return binding.activityTripRecordingDrivingModeButton
    }

    override fun getMapLayerButton(): FloatingActionButton {
        return binding.activityTripRecordingLayerButton
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SAVE_TRIP && resultCode == RESULT_OK) {
            finish()
        } else if (requestCode == REQUEST_EDIT_POI && resultCode == ActivityResult.RESULT_OBJECT_UPDATED) {
            Log.d(TAG, "POI updated")
        } else if (requestCode == REQUEST_EDIT_POI && resultCode == ActivityResult.RESULT_OBJECT_DELETED) {
            Log.d(TAG, "POI deleted")
        } else {
            Log.w(TAG, "Something went wrong for child activity.")
        }

    }

    // Location Activity Abstract Methods Implementation

    override fun onLocationServiceBind() {
        initializeGpsLocationUpdates()
    }

    override fun onLocationPermissionsGranted() {
        tryRegisteringLocationObservers()
    }

    override fun onLocationUpdated(location: Location) {
        Log.i(TAG, "ILocationUpdateListener.onLocationUpdated()")
        Log.d(TAG, location.toString())
        viewModel.setLastLocation(location)
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

    override fun updateBackButton() {
        val backButtonEnabled = !viewModel.getTripRecorder(this).hasActiveTripRecording()
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(backButtonEnabled)
            setHomeButtonEnabled(backButtonEnabled)
        }
    }

    override fun getTrailId(): Long? {
        return null // There is no trail ID for free roam recording
    }

    override fun getTripUuid(): String? {
        return viewModel.tripUUID
    }

    override fun getExtProperties(): List<ExtProperties>? {
        return null
    }

    override fun onNewTripUuid(tripUuid: String?) {
        viewModel.tripUUID = tripUuid
    }

    override fun getAddPoiButton(): View {
        return binding.activityTripRecordingAddPoiButton
    }

    override fun getRecButtonBinding(): ComponentTripRecordingButtonsBinding {
        return binding.activityTripRecordingButtons
    }

    override fun getSnackbarView(): View {
        return binding.activityTripRecordingRecordingPanel
    }

    /* PRIVATE */

    private fun initRecordingPanel() {

        val stats = binding.activityTripRecordingStats
        val buttons = binding.activityTripRecordingButtons

        buttonsPanel = TripRecordingButtonsPanel(buttons.componentTripRecordingStatDuration,
            buttons.componentTripRecordingStatDistance, buttons.componentTripRecordingStartButton,
            buttons.componentTripRecordingStopButton, buttons.componentTripRecordingResumeButton,
            buttons.componentTripRecordingFinishButton, binding.activityTripRecordingAddPoiButton, this)

        statsPanel = TripRecordingStatsPanel(
            stats.componentTripRecordingStatsSpeed, stats.componentTripRecordingStatsHeading,
            stats.componentTripRecordingStatsElevation, stats.componentTripRecordingStatsLat,
            stats.componentTripRecordingStatsLon, this
        )

    }

    private fun setupToolbar() {
        val toolbar = binding.activityTripRecordingToolbar
        setSupportActionBar(toolbar.generalToolbar)
        toolbar.generalToolbarTitle.text = getString(R.string.activity_new_free_roam_trip_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    /* -----------  RECORDING STUFF  ---------- */

    override fun setupButtons() {
        // Init recording buttons
        super.setupButtons()

        binding.activityTripRecordingLayerButton.setOnClickListener {
            onToggleMapStyle()
        }
    }

    /* -----------  END OF RECORDING STUFF  ---------- */

    /* -----------  MAP STUFF  ---------- */

    private fun onAccuTerraMapViewReady() {
        Log.i(TAG, "onAccuTerraMapViewReady()")
        // Try to register location observers to obtain device location
        tryRegisteringLocationObservers()

        lifecycleScope.launchWhenResumed {
            setupRecordingAfterOnAccuTerraMapViewReady()
            updateBackButton()
            switchToLocationMode()
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
            // Display recording button
            binding.activityTripRecordingLayerButton.show()
        }
    }

    private fun tryRegisteringLocationObservers() {
        if (hasLocationPermissions()) {
            registerLocationObservers()
            registerStatisticsObserver()
            // Start requesting location updates
            startLocationUpdates()
        }
    }

    private fun switchToLocationMode() {
        Log.i(TAG, "switchToLocationMode()")
        getAccuTerraMapView().setTracking(TrackingOption.LOCATION, null)
    }

    private fun registerLocationObservers() {
        getLastLocationLiveData().observe(this, { lastLocation ->
            updateLocationOnMap(lastLocation)
        })
    }

    private class AccuTerraMapViewListener(activity: TripRecordingActivity)
        : AccuTerraMapView.IAccuTerraMapViewListener {

        private val weakActivity= WeakReference(activity)

        override fun onInitialized(mapboxMap: MapboxMap) {
            Log.i(TAG, "AccuTerraMapViewListener.onInitialized()")

            val activity = weakActivity.get()
                ?: return

            activity.onAccuTerraMapViewReady()
        }

        override fun onStyleChanged(mapboxMap: MapboxMap) {
            weakActivity.get()?.let { activity ->
                activity.getMapLayerButton().isEnabled = true
                activity.getDrivingModeButton().isEnabled = true
                // Reload recorded trip path after the style has changed
                activity.lifecycleScope.launchWhenResumed {
                    activity.getAccuTerraMapView().tripLayersManager.reloadTripRecorderData()
                }
            }
        }

        override fun onSignificantMapBoundsChange() {
            // nothing to do. only the GPS data matters - not the map bounds
        }

        override fun onTrackingModeChanged(mode: TrackingOption) {
            // TODO #16292 - anything we want to do here?
        }
    }

    private class MapLoadingFailListener(activity: TripRecordingActivity) :
        MapView.OnDidFailLoadingMapListener {

        private val weakActivity = WeakReference(activity)

        override fun onDidFailLoadingMap(errorMessage: String?) {
            weakActivity.get()?.let { context ->
                DialogUtil.buildOkDialog(
                    context, "Error",
                    errorMessage ?: "Unknown Error While Loading Map"
                )
            }
        }
    }

}
