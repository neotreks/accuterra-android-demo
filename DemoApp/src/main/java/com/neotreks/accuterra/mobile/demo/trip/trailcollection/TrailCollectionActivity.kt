package com.neotreks.accuterra.mobile.demo.trip.trailcollection

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityTrailCollectionBinding
import com.neotreks.accuterra.mobile.demo.databinding.ComponentTripRecordingButtonsBinding
import com.neotreks.accuterra.mobile.demo.media.ApkMediaUtil
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.demo.trip.recorded.BaseTripRecordingActivity
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripRecordingButtonsPanel
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripRecordingStatsPanel
import com.neotreks.accuterra.mobile.demo.util.ActivityResult
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.SdkManager
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.model.ExtProperties
import com.neotreks.accuterra.mobile.sdk.model.ExtPropertiesBuilder
import com.neotreks.accuterra.mobile.sdk.telemetry.model.TelemetryModel
import com.neotreks.accuterra.mobile.sdk.trail.extension.toMapLocation
import com.neotreks.accuterra.mobile.sdk.trail.model.MapLocation
import com.neotreks.accuterra.mobile.sdk.trail.model.PointType
import com.neotreks.accuterra.mobile.sdk.trail.model.Tag
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailCollectionData
import com.neotreks.accuterra.mobile.sdk.trip.model.*
import com.neotreks.accuterra.mobile.sdk.trip.recorder.ITripRecorder
import com.neotreks.accuterra.mobile.sdk.trip.recorder.ITripRecorderListener
import com.neotreks.accuterra.mobile.sdk.trip.recorder.TripRecorderStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class TrailCollectionActivity : BaseTripRecordingActivity(),
    TrailCollectionPoiFragment.TrailCollectionPoiFragmentListener {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: TrailCollectionViewModel

    private val accuTerraMapViewListener = AccuTerraMapViewListener(this)
    private val mapViewLoadingFailListener = MapLoadingFailListener(this)

    private lateinit var binding: ActivityTrailCollectionBinding

    private val recorderListener: ITripRecorderListener = object: ITripRecorderListener {
        override suspend fun onLocationAdded(location: Location) {}
        override suspend fun onPoiAdded(tripPoi: TripRecordingPoi) {}

        override suspend fun onPoiUpdated(tripPoi: TripRecordingPoi) {}

        override suspend fun onPoiDeleted(poiUuid: String) {}

        override suspend fun onRecentLocationBufferChanged(newLocation: Location, bufferFlushed: Boolean) {}

        override suspend fun onStatusChanged(status: TripRecorderStatus) {
            displayRecordingStatus(status)
            updateBackButton()
        }
    }

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val TAG = "RecordingActivity"

        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, TrailCollectionActivity::class.java)
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrailCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(TrailCollectionViewModel::class.java)
        lifecycleScope.launchWhenCreated {
            viewModel.initTagMapping(this@TrailCollectionActivity)
        }

        initRecordingPanel()
        setupToolbar()
        setupButtons()

        // Let's setup the map as the last component
        setupMap(savedInstanceState)
        // Set POI Fragment
        setupPoiFragment()
        setRecordingButtonsColor()
        displayRecordingStatus(viewModel.getTripRecorder(this).getStatus())
    }

    override fun onStart() {
        super.onStart()
        viewModel.getTripRecorder(this).addListener(recorderListener)
    }

    override fun onStop() {
        super.onStop()
        viewModel.getTripRecorder(this).removeListener(recorderListener)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menu?.let {
            menuInflater.inflate(R.menu.menu_trail_collection, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.trail_collection_menu_item_display_raw -> {
                SdkManager.debugConfig.tripConfiguration.displayRawLocations = !item.isChecked
                lifecycleScope.launchWhenResumed {
                    getAccuTerraMapView().tripLayersManager.reloadTripRecorderData()
                }
            }
            R.id.trail_collection_menu_item_display_guide -> {
                onShowGuide()
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.trail_collection_menu_item_display_raw)
        item.isChecked = SdkManager.debugConfig.tripConfiguration.displayRawLocations
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onBackPressed() {
        val canNavigateBack = canNavigateBack()
        if (!canNavigateBack) {
            if (viewModel.isAddPoiMode) {
                onPoiPanelBackClicked()
                return
            } else {
                toast(getString(R.string.general_recording_cannot_exit_while_recording))
                return
            }
        }
        super.onBackPressed()
    }

    /* Customizations of BaseTripRecordingActivity */

    override fun onAddPoiClicked() {
        viewModel.onAddPoiClicked()
        setModePoi(viewModel.lastLocation.value?.toMapLocation())
    }

    override fun canHandleTripPoiMapClick(): Boolean {
        return !viewModel.isAddPoiMode
    }

    override fun onEditPoiClicked(poiUuid: String) {
        lifecycleScope.launchWhenResumed {
            viewModel.loadPoi(poiUuid, this@TrailCollectionActivity)
            setModePoi(viewModel.poi.value?.mapLocation)
        }
    }

    override fun buildOnRecordingFinishedIntent(uuid: String): Intent {
        return TrailSaveActivity.createNavigateToIntent(this, uuid)
    }

    /* IMPLEMENTATION OF ABSTRACT METHODS */

    override fun getAccuTerraMapView(): AccuTerraMapView {
        return binding.activityTrailCollectionAccuterraMapView
    }

    override fun getAccuTerraMapViewListener(): AccuTerraMapView.IAccuTerraMapViewListener {
        return accuTerraMapViewListener
    }

    override fun getMapViewLoadingFailListener(): MapView.OnDidFailLoadingMapListener {
        return mapViewLoadingFailListener
    }

    override fun getDrivingModeButton(): FloatingActionButton {
        return binding.activityTrailCollectionDrivingModeButton
    }

    override fun getMapLayerButton(): FloatingActionButton {
        return binding.activityTrailCollectionLayerButton
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
        val backButtonEnabled = canNavigateBack()
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

    override fun getExtProperties(): List<ExtProperties> {
        return ExtPropertiesBuilder.buildList(TrailCollectionData())
    }

    override fun getTelemetryModel(): TelemetryModel? {
        return null
    }

    override fun onNewTripUuid(tripUuid: String?) {
        viewModel.tripUUID = tripUuid
    }

    override fun getAddPoiButton(): View {
        return binding.activityTrailCollectionAddPoiButton
    }

    override fun getRecButtonBinding(): ComponentTripRecordingButtonsBinding {
        return binding.activityTrailCollectionButtons
    }

    override fun getSnackbarView(): View {
        return binding.activityTrailCollectionRecordingPanel
    }

    override fun setupButtons() {
        // Init recording buttons
        super.setupButtons()

        binding.activityTrailCollectionLayerButton.setOnClickListener {
             onToggleMapStyle()
        }

        binding.activityTrailCollectionGuideButton.setOnClickListener {
            onShowGuide()
        }
    }

    // FRAGMENT STUFF

    override fun isAddPoiMode(): Boolean {
        return viewModel.isAddPoiMode
    }

    override fun onSavePoi(
        name: String,
        description: String,
        pointType: PointType,
        tags: List<Tag>,
        media: MutableList<TripRecordingMedia>
    ) {

        val mapCenter = binding.activityTrailCollectionAccuterraMapView.getMapboxMap().cameraPosition.target
        val mapLocation = MapLocation(mapCenter.latitude, mapCenter.longitude)

        val dialog = DialogUtil.buildBlockingProgressDialog(this, getString(R.string.general_saving))
        dialog.show()

        GlobalScope.launch {

            // Gather POI data
            val enumService = ServiceFactory.getEnumService(applicationContext)

            val viewModelPoi = viewModel.poi.value

            var addPoi = false
            val poi = if (viewModelPoi == null) {
                addPoi = true
                // Build the new POI
                TripRecordingPoiBuilder.buildNewPoi(
                    name = name,
                    description = description,
                    mapLocation = mapLocation,
                    descriptionShort = null,
                    media = media, // Just default value
                    pointTypeCode = pointType.code,
                    tags = tags,
                    enumService = enumService
                )
            } else {
                viewModelPoi.copy(
                    name = name,
                    description = description,
                    mapLocation = mapLocation,
                    descriptionShort = null,
                    media = media, // Just default value
                    pointType = pointType,
                    tags = tags
                )
            }

            if (addPoi) {
                // Add the newly created POI to the trip recording
                viewModel.addPoi(poi, applicationContext)
            } else {
                // Update the existing POI
                viewModel.updatePoi(poi, applicationContext)
            }

            // We need to cleanup the camera folder since images were copies into SDK and are not needed anymore
            ApkMediaUtil.clearCameraTempFolder(this@TrailCollectionActivity)

            // Close the dialog
            lifecycleScope.launchWhenCreated {
                dialog.dismiss()
                setModeStandard()
            }

        }

    }

    override fun onPoiPanelBackClicked() {
        if (viewModel.isFormDirty) {
            DialogUtil.buildYesNoDialog(
                context = this,
                title = getString(R.string.activity_trail_collection_close_form_title),
                message = getString(R.string.activity_trail_collection_close_form_message),
                positiveCode = {
                    setModeStandard()
                }
            ).show()
        } else {
            setModeStandard()
        }
    }

    override fun getCurrentLocation(): Location? {
        return viewModel.lastLocation.value
    }

    /* PRIVATE */

    /* -----------  FRAGMENT STUFF  ---------- */

    private fun setupPoiFragment() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            disallowAddToBackStack() // This will be without back navigation
            val fragment = TrailCollectionPoiFragment()
            add(R.id.activity_trail_collection_poi_panel, fragment)
        }
    }

    /* -----------  RECORDING STUFF  ---------- */

    private fun setRecordingButtonsColor() {
        val background = ContextCompat.getDrawable(this, R.drawable.background_circle_trail_collection_primary_color)
        val buttons = binding.activityTrailCollectionButtons
        setButtonBackground(buttons.componentTripRecordingStartButton, background)
        setButtonBackground(buttons.componentTripRecordingStopButton, background)
        setButtonBackground(buttons.componentTripRecordingResumeButton, background)
        setButtonBackground(buttons.componentTripRecordingFinishButton, background)
    }

    private fun setButtonBackground(button: Button, background: Drawable?) {
        button.background = background
        button.backgroundTintList = null
    }

    private fun displayRecordingStatus(status: TripRecorderStatus) {
        val label = binding.activityTrailCollectionStatusPanel
        when (status) {
            TripRecorderStatus.RECORDING -> {
                label.visibility = true.visibility
                label.text = getString(R.string.activity_trail_collection_recording_status_recording)
                label.setBackgroundColor(getColor(R.color.trailCollectionStatusRecording))
            }
            TripRecorderStatus.PAUSED -> {
                label.visibility = true.visibility
                label.text = getString(R.string.activity_trail_collection_recording_status_paused)
                label.setBackgroundColor(getColor(R.color.trailCollectionStatusPaused))
            }
            else -> {
                label.visibility = false.visibility
            }
        }
    }

    /* -----------  END OF RECORDING STUFF  ---------- */

    private fun initRecordingPanel() {

        val stats = binding.activityTrailCollectionStats
        val buttons = binding.activityTrailCollectionButtons

        buttonsPanel = TripRecordingButtonsPanel(
            buttons.componentTripRecordingStatDuration,
            buttons.componentTripRecordingStatDistance, buttons.componentTripRecordingStartButton,
            buttons.componentTripRecordingStopButton, buttons.componentTripRecordingResumeButton,
            buttons.componentTripRecordingFinishButton, binding.activityTrailCollectionAddPoiButton, this
        )

        statsPanel = TripRecordingStatsPanel(
            stats.componentTripRecordingStatsSpeed, stats.componentTripRecordingStatsHeading,
            stats.componentTripRecordingStatsElevation, stats.componentTripRecordingStatsLat,
            stats.componentTripRecordingStatsLon, this
        )

    }

    private fun setupToolbar() {
        val toolbar = binding.activityTrailCollectionToolbar
        setSupportActionBar(toolbar.trailCollectionToolbar)
        toolbar.trailCollectionToolbarTitle.text = getString(R.string.activity_trail_collection_toolbar_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    /* -----------  MAP STUFF  ---------- */

    private fun onAccuTerraMapViewReady() {
        Log.i(TAG, "onAccuTerraMapViewReady()")

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
            binding.activityTrailCollectionLayerButton.show()
            // Try to register location observers to obtain device location
            tryRegisteringLocationObservers()
            // Make sure we display right UI at the beginning or after the configuration change
            if (viewModel.isAddPoiMode) {
                setModePoi(viewModel.addPoiLocation?.toMapLocation() ?: viewModel.lastLocation.value?.toMapLocation())
            } else {
                setModeStandard()
            }
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

    private fun onShowGuide() {
        val intent = TrailCollectionGuideActivity.createNavigateToIntent(this)
        startActivity(intent)
    }

    private fun setModePoi(location: MapLocation?) {
        // Mark POI flag in view model
        viewModel.isAddPoiMode = true
        // Hide Add POI button
        binding.activityTrailCollectionAddPoiButton.visibility = false.visibility
        // Resize map frame to top
        val mapFrame = binding.activityTrailCollectionMapFrame
        val params = mapFrame.layoutParams as ConstraintLayout.LayoutParams
        params.bottomToTop = binding.activityTrailCollectionTopAnchor.id
        mapFrame.requestLayout()
        if (location != null) {
            val update = CameraUpdateFactory.newLatLng(location.toLatLng())
            getAccuTerraMapView().getMapboxMap().animateCamera(update, 2_000)
        }
        // Display Map Crosshair
        binding.activityTrailCollectionCrossHair.visibility = true.visibility
        // Display POI form
        binding.activityTrailCollectionPoiPanel.visibility = true.visibility
    }

    private fun setModeStandard() {
        val hasActiveRec = viewModel.getTripRecorder(this).hasActiveTripRecording()
        binding.activityTrailCollectionAddPoiButton.visibility = hasActiveRec.visibility
        viewModel.isAddPoiMode = false
        viewModel.poiBackup = null
        val mapFrame = binding.activityTrailCollectionMapFrame
        val params = mapFrame.layoutParams as ConstraintLayout.LayoutParams
        params.bottomToTop = binding.activityTrailCollectionRecordingPanel.id
        mapFrame.requestLayout()
        // Hide POI form
        binding.activityTrailCollectionPoiPanel.visibility = false.visibility
        // Hide Crosshair
        binding.activityTrailCollectionCrossHair.visibility = false.visibility
    }

    private fun canNavigateBack(): Boolean {
        // We can navigate back if there is no active recording
        val status = viewModel.getTripRecorder(this).getStatus()
        return status != TripRecorderStatus.RECORDING && status != TripRecorderStatus.PAUSED
    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    private class AccuTerraMapViewListener(activity: TrailCollectionActivity)
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

    private class MapLoadingFailListener(activity: TrailCollectionActivity) :
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
