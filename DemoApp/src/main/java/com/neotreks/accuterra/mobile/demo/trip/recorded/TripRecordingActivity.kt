package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.*
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
import com.neotreks.accuterra.mobile.demo.trip.TestTelemetryRecording
import com.neotreks.accuterra.mobile.demo.util.ActivityResult
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.sdk.SdkManager
import com.neotreks.accuterra.mobile.sdk.map.AccuTerraMapView
import com.neotreks.accuterra.mobile.sdk.map.TrackingOption
import com.neotreks.accuterra.mobile.sdk.model.ExtProperties
import com.neotreks.accuterra.mobile.sdk.telemetry.model.TelemetryModel
import com.neotreks.accuterra.mobile.sdk.telemetry.model.TelemetryModelBuilder
import com.neotreks.accuterra.mobile.sdk.telemetry.model.TelemetryRecordType
import com.neotreks.accuterra.mobile.sdk.telemetry.model.TelemetryValuesBuilder
import com.neotreks.accuterra.mobile.sdk.trip.recorder.ITripRecorder
import com.neotreks.accuterra.mobile.sdk.trip.model.TripStatistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.random.Random

class TripRecordingActivity : BaseTripRecordingActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: TripRecordingViewModel

    private val accuTerraMapViewListener = AccuTerraMapViewListener(this)
    private val mapViewLoadingFailListener = MapLoadingFailListener(this)

    private lateinit var binding: ActivityTripRecordingBinding

    private val demoTelemetryModel: TelemetryModel by lazy { buildTelemetryModel() }
    private val demoTelemetryType = demoTelemetryModel.recordTypes.first()
    private var telemetryTimer: Timer? = null

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
        when (item.itemId) {
            R.id.trip_record_menu_item_display_raw -> {
                SdkManager.debugConfig.tripConfiguration.displayRawLocations = !item.isChecked
                lifecycleScope.launchWhenResumed {
                    getAccuTerraMapView().tripLayersManager.reloadTripRecorderData()
                }
            }
            R.id.trip_record_menu_item_record_random_telemetry -> {
                SdkManager.debugConfig.tripConfiguration.recordRandomTelemetry = !item.isChecked
            }
            R.id.trip_record_menu_item_telemetry_performance_test -> {
                testTelemetryPerformance()
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val rawPathItem = menu.findItem(R.id.trip_record_menu_item_display_raw)!!
        rawPathItem.isChecked = SdkManager.debugConfig.tripConfiguration.displayRawLocations
        val telemetryItem = menu.findItem(R.id.trip_record_menu_item_record_random_telemetry)!!
        telemetryItem.isChecked = SdkManager.debugConfig.tripConfiguration.recordRandomTelemetry
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onStart() {
        super.onStart()
        // Example of logging random telemetry data
        telemetryTimer = fixedRateTimer(period = 500L) {
            if(!SdkManager.debugConfig.tripConfiguration.recordRandomTelemetry) {
                return@fixedRateTimer
            }
            lifecycleScope.launchWhenCreated {
                try {
                    val recorder = viewModel.getTripRecorder(this@TripRecordingActivity)
                    // We log only when there is an active recording
                    if (!viewModel.getTripRecorder(this@TripRecordingActivity).hasActiveTripRecording()) {
                        return@launchWhenCreated
                    }
                    for (i in 0..5) {
                        val values = buildRandomTelemetryValues(demoTelemetryType)
                        recorder.logTelemetry(demoTelemetryType, values)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error while recording telmetry because of: ${e.localizedMessage}")
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        telemetryTimer?.cancel()
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

    override fun getTelemetryModel(): TelemetryModel? {
        return demoTelemetryModel
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

        buttonsPanel = TripRecordingButtonsPanel(
            buttons.componentTripRecordingStatDuration,
            buttons.componentTripRecordingStatDistance, buttons.componentTripRecordingStartButton,
            buttons.componentTripRecordingStopButton, buttons.componentTripRecordingResumeButton,
            buttons.componentTripRecordingFinishButton, binding.activityTripRecordingAddPoiButton, this
        )

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

    private fun buildTelemetryModel(): TelemetryModel {
        val telemetryTypeName = "locations"
        val builder = TelemetryModelBuilder.create("test_telemetry", "neo", 0)
        builder.addRecordType(telemetryTypeName)
            .addDoubleField("lat", required = true)
            .addDoubleField("lon", required = true)
            .addDoubleField("alt", required = false)

        val model = builder.build()
        return model
    }

    private fun buildRandomTelemetryValues(recordType: TelemetryRecordType): ContentValues {
        val builder = TelemetryValuesBuilder.create(recordType, System.currentTimeMillis())
        builder.put("lat", Random.nextDouble(-90.0, 90.0))
        builder.put("lon", Random.nextDouble(-180.0, 180.0))
        builder.put("alt", Random.nextDouble(0.0, 2000.0))
        return builder.build()
    }

    private class AccuTerraMapViewListener(activity: TripRecordingActivity) :
        AccuTerraMapView.IAccuTerraMapViewListener {

        private val weakActivity = WeakReference(activity)

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

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun testTelemetryPerformance() {
        val dialog = DialogUtil.buildYesNoDialog(
            context = this,
            title = getString(R.string.activity_trip_recording_telemetry_test_dialog_title),
            message = getString(R.string.activity_trip_recording_telemetry_test_dialog_message_scope),
            positiveCodeLabel = getString(R.string.general_dispatcher_io),
            positiveCode = { testTelemetryPerformance(true) },
            negativeCodeLabel = getString(R.string.general_dispatcher_default),
            negativeCode = { testTelemetryPerformance(false) },
        )
        dialog.show()
    }

    private fun testTelemetryPerformance(fromIO: Boolean) {
        val test = TestTelemetryRecording(this)
        lifecycleScope.launchWhenCreated {
            val dialog = DialogUtil.buildBlockingProgressDialog(
                this@TripRecordingActivity,
                getString(R.string.activity_trip_recording_telemetry_test_dialog_title),
            )
            dialog.show()
            val result = if (fromIO) {
                withContext(Dispatchers.IO) {
                    test.testLogTelemetry(getString(R.string.general_dispatcher_io))
                }
            } else {
                withContext(Dispatchers.Default) {
                    test.testLogTelemetry(getString(R.string.general_dispatcher_default))
                }
            }
            dialog.dismiss()
            // Result dialog
            val resultMessage = if (result.isSuccess) {
                result.value ?: "No result"
            } else {
                result.errorMessage ?: "Unknown error"
            }
            val resultDialog = DialogUtil.buildYesNoDialog(
                context = this@TripRecordingActivity,
                title = getString(R.string.activity_trip_recording_telemetry_test_dialog_title),
                message = resultMessage,
                positiveCodeLabel = getString(R.string.general_ok),
                negativeCodeLabel = getString(R.string.general_copy_to_clipboard),
                negativeCode = {
                    // Gets a handle to the clipboard service.
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    // Creates a new text clip to put on the clipboard
                    val clip: ClipData = ClipData.newPlainText("Telemetry test", resultMessage)
                    // Put into the clipboard
                    clipboard.setPrimaryClip(clip)
                }
            )
            resultDialog.show()
        }
    }

}
