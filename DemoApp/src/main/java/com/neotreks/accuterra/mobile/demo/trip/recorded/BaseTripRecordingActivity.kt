package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Intent
import android.location.Location
import android.util.Log
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.mapbox.mapboxsdk.geometry.LatLng
import com.neotreks.accuterra.mobile.demo.BaseDrivingActivity
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ComponentTripRecordingButtonsBinding
import com.neotreks.accuterra.mobile.demo.extensions.toLocalDateString
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.sdk.util.LocationPermissionUtil
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.map.TripLayersManager
import com.neotreks.accuterra.mobile.sdk.map.query.TripPoisQueryBuilder
import com.neotreks.accuterra.mobile.sdk.model.ExtProperties
import com.neotreks.accuterra.mobile.sdk.telemetry.model.TelemetryModel
import com.neotreks.accuterra.mobile.sdk.trip.recorder.ITripRecorder
import com.neotreks.accuterra.mobile.sdk.trip.recorder.TripStartResultType
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingStatus
import com.neotreks.accuterra.mobile.sdk.trip.model.TripStatistics
import kotlinx.coroutines.*
import java.util.*

/**
 *  Base activity for `trip recording screens` containing map with recording.
 */
abstract class BaseTripRecordingActivity : BaseDrivingActivity() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "BaseTripRecActivity"

        const val REQUEST_ADD_POI = 2
        const val REQUEST_EDIT_POI = 3
        const val REQUEST_SAVE_TRIP = 4
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    protected lateinit var tripLayersManager: TripLayersManager
    protected var isTripLayersManagerLoaded = false

    protected lateinit var buttonsPanel: TripRecordingButtonsPanel
    protected lateinit var statsPanel: TripRecordingStatsPanel

    protected var lastSnackbar: Snackbar? = null

    /* * * * * * * * * * * * */
    /*       ABSTRACT        */
    /* * * * * * * * * * * * */

    protected abstract fun getTrailId(): Long?

    protected abstract fun getTripUuid(): String?

    /**
     * Provide custom trip recording data to be stored with the trip recording
     */
    protected abstract fun getExtProperties(): List<ExtProperties>?

    /**
     * Provide [TelemetryModel] only if the telemetry will be recorded
     */
    protected abstract fun getTelemetryModel(): TelemetryModel?

    protected abstract suspend fun getTripRecorder(): ITripRecorder

    protected abstract fun getTripStatisticsLiveData(): MutableLiveData<TripStatistics?>

    protected abstract fun getAddPoiButton(): View

    protected abstract fun getRecButtonBinding(): ComponentTripRecordingButtonsBinding

    protected abstract fun updateBackButton()

    protected abstract fun onNewTripUuid(tripUuid: String?)

    /** Should returns view where the snackbar can be displayed */
    protected abstract fun getSnackbarView() : View

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onDestroy() {
        // Example of trying to flush recorder's buffer to persist in-memory data.
        //
        // In real application where the recorder is going to be used e.g. in a foreground service
        // it is recommended to use the [ITripRecorder.flushTripRecordingBuffer] method
        // in the [Service.onTaskRemoved] and [Service.onTrimMemory] methods.
        GlobalScope.launch(Dispatchers.Default) {
            getTripRecorder().flushTripRecordingBuffer()
        }
        // Check if we need location updates
        runBlocking {
            if(getTripRecorder().getActiveTripRecording()?.recordingInfo?.status != TripRecordingStatus.RECORDING) {
                stopLocationUpdates()
            }
        }
        // Call super method
        super.onDestroy()
    }

    override fun updateLocationOnMap(location: Location?) {
        super.updateLocationOnMap(location)
        // Pass the new location to the UI
        statsPanel.updateRecordingUI(location)
    }

    /* * * * * * * * * * * * */
    /*       PROTECTED       */
    /* * * * * * * * * * * * */

    protected suspend fun setupRecordingAfterOnAccuTerraMapViewReady() {
        addTripLayers()
        registerMapClickHandler()
        updateRecordingButtons()
    }

    protected fun registerStatisticsObserver() {
        getTripStatisticsLiveData().observe(this, { statistics ->
            buttonsPanel.updateStatistics(statistics?.length, statistics?.drivingTime)
        })
    }

    protected open suspend fun updateRecordingButtons() {

        val activeTrip = getTripRecorder().getActiveTripRecording()
        val status = activeTrip?.recordingInfo?.status

        when(status) {
            TripRecordingStatus.RECORDING -> buttonsPanel.setRecordingMode()
            TripRecordingStatus.PAUSED -> buttonsPanel.setPausedMode()
            else -> buttonsPanel.setStartMode()
        }

        Log.d(TAG, "Buttons updated with status: $status")
    }

    override fun setupButtons() {
        super.setupButtons()

        getAddPoiButton().setOnClickListener {
            onAddPoiClicked()
        }

        // Expecting usage of the: component_trip_recording_buttons

        val bindingButton = getRecButtonBinding()

        bindingButton.componentTripRecordingStartButton.setOnClickListener {
            lifecycleScope.launchWhenResumed {
                if (LocationPermissionUtil.hasBackgroundLocationPermission(this@BaseTripRecordingActivity)) {
                    DialogUtil.buildYesNoDialog(
                        context = this@BaseTripRecordingActivity,
                        title = getString(R.string.activity_trip_recording_start_dialog_title),
                        message = getString(R.string.activity_trip_recording_start_dialog_message),
                        positiveCode = {
                            lifecycleScope.launchWhenResumed {
                                onRecordingStartClicked()
                            }
                        }
                    ).show()
                } else {
                    LocationPermissionUtil.checkBackgroundLocationPermissions(this@BaseTripRecordingActivity, REQUEST_PERMISSIONS_REQUEST_CODE)
                }
            }
        }
        bindingButton.componentTripRecordingStopButton.setOnClickListener {
            lifecycleScope.launchWhenResumed {
                onRecordPauseClicked()
            }
        }
        bindingButton.componentTripRecordingResumeButton.setOnClickListener {
            lifecycleScope.launchWhenResumed {
                onRecordingResumeClicked()
            }
        }
        bindingButton.componentTripRecordingFinishButton.setOnClickListener {
            lifecycleScope.launchWhenResumed {
                onRecordingFinishClicked()
            }
        }
    }

    protected open suspend fun onRecordingStartClicked() {
        val recorder = getTripRecorder()
        val activeTrip = recorder.getActiveTripRecording()

        tripLayersManager.setTripRecorder(recorder)

        // Handle different statuses
        when(activeTrip?.recordingInfo?.status) {
            // Start new recording
            null -> {
                val tripName = "Trip ${Date().toLocalDateString()}" // Default name
                lifecycleScope.launchWhenResumed {
                            val vehicleId = "test_vehicle" // TODO: Load vehicle ID
                            val result = recorder.startTripRecording(tripName, getTrailId(), vehicleId,
                                getExtProperties(), getTelemetryModel()
                            )
                            when {
                                result.isFailure -> {
                                    toast(getString(R.string.activity_trip_recording_trip_recording_start_error, result.errorMessage))
                                }
                                result.value?.startType == TripStartResultType.OK_NEW_STARTED -> {
                                    onNewTripUuid(result.value?.tripUuid)
                                }
                                else -> {
                                    toast(getString(R.string.general_unexpected_error))
                                }
                            }
                            // Disable back navigation if needed and setup buttons
                            updateBackButton()
                            updateRecordingButtons()
                            // Start recording
                            startLocationRecording()
                        }
            }
            else -> {
                toast(getString(R.string.activity_trip_recording_recording_start_failed,
                    activeTrip.recordingInfo.status))
            }
        }
    }

    protected open suspend fun onRecordPauseClicked() {
        val recorder = getTripRecorder()
        val activeTrip = recorder.getActiveTripRecording()

        // Handle different statuses
        when(activeTrip?.recordingInfo?.status) {
            // From Recording to Paused
            TripRecordingStatus.RECORDING -> {
                recorder.pauseTripRecording()
                stopLocationRecording()
                buttonsPanel.setPausedMode()
            }
            else -> {
                toast(getString(R.string.activity_trip_recording_recording_pause_failed, activeTrip?.recordingInfo?.status))
            }
        }
        // Update the UI
        updateRecordingButtons()
    }

    protected open suspend fun onRecordingResumeClicked() {
        val recorder = getTripRecorder()
        val activeTrip = recorder.getActiveTripRecording()

        // Handle different statuses
        when(activeTrip?.recordingInfo?.status) {
            // From Paused to Recording
            TripRecordingStatus.PAUSED -> {
                recorder.resumeTripRecording()
                startLocationRecording()
            }
            else -> {
                toast(getString(R.string.activity_trip_recording_recording_resume_failed, activeTrip?.recordingInfo?.status))
            }
        }
        // Update the UI
        updateRecordingButtons()
    }

    protected open suspend fun onRecordingFinishClicked() {
        DialogUtil.buildYesNoDialog(this,
            title = getString(R.string.activity_trip_recording_finish_dialog_title),
            message = getString(R.string.activity_trip_recording_finish_dialog_message),
            positiveCode = {
                lifecycleScope.launchWhenCreated {
                    onRecordingFinishConfirmed()
                }
            }
        ).show()
    }

    protected open suspend fun onRecordingFinishConfirmed() {
        val recorder = getTripRecorder()
        val uuid = recorder.getActiveTripRecording()?.tripInfo?.uuid
            ?: throw IllegalStateException("No trip UUID available.")
        stopLocationRecording()
        // We want to copy tags from POIs to Trip itself
        val pois = ServiceFactory.getTripRecordingService(this).getTripRecordingPOIs(uuid)
        val poisTags = pois.flatMap { it.tags }
        val trailTags = ServiceFactory.getEnumService(applicationContext).getCorrespondingTrailTags(poisTags)
        recorder.setTripRecordingTags(trailTags)
        // Now we want to finish trip recording
        recorder.finishTripRecording()
        updateRecordingButtons()
        getAccuTerraMapView().tripLayersManager.setTripRecorder(null)
        val intent = buildOnRecordingFinishedIntent(uuid)
        startActivityForResult(intent, REQUEST_SAVE_TRIP)
    }

    protected open fun buildOnRecordingFinishedIntent(uuid: String): Intent {
        return TripSaveActivity.createNavigateToIntent(this@BaseTripRecordingActivity, uuid)
    }

    protected open fun onAddPoiClicked() {
        val poiLocation = getLastLocationLiveData().value
            ?: return

        val tripUuid = getTripUuid()
            ?: return

        val intent = TripAddPoiActivity.createNavigateToIntent(this, tripUuid, poiLocation, useTripRecorded = true)
        startActivityForResult(intent, REQUEST_ADD_POI)

    }

    protected open fun onEditPoiClicked(poiUuid: String) {
        val intent = TripEditPoiActivity.createNavigateToIntent(poiUuid, this)
        startActivityForResult(intent, REQUEST_EDIT_POI)
    }

    protected open fun canHandleTripPoiMapClick(): Boolean {
        return true
    }

    protected suspend fun addTripLayers() {
        Log.d(TAG, "addTripLayers()")

        withContext(Dispatchers.Main) {
            tripLayersManager = getAccuTerraMapView().tripLayersManager
            tripLayersManager.addStandardTripLayers()
            isTripLayersManagerLoaded = true
            val recorder = getTripRecorder()
            if (recorder.hasActiveTripRecording()) {
                tripLayersManager.setTripRecorder(recorder)
            }
        }
    }

    /* * * * * * * * * * * * */
    /*  MAP CLICK HANDLING   */
    /* * * * * * * * * * * * */

    protected fun registerMapClickHandler() {
        getAccuTerraMapView().getMapboxMap().addOnMapClickListener { latLng ->
            handleMapViewClick(latLng)
            return@addOnMapClickListener true
        }
    }

    protected open fun handleMapViewClick(latLng: LatLng) : Boolean {
        return searchTripPois(latLng)
    }

    private fun searchTripPois(latLng: LatLng) : Boolean {
        val searchResult = TripPoisQueryBuilder(tripLayersManager)
            .setCenter(latLng)
            .setTolerance(5.0f)
            .includeAllTripLayers()
            .create()
            .execute()

        when(searchResult.tripPois.size) {
            0 -> {
                showNewSnackBar(null)
                tripLayersManager.highlightPOI(null)
                return false
            }
            1 -> {
                val tripPois = searchResult.tripPois[0]

                when(tripPois.poiIUuids.size) {
                    0 -> throw IllegalStateException("Search returned a layer with an empty POIs set.")
                    1 -> {
                        handleTripPoiMapClick(tripPois.tripUuid, tripPois.poiIUuids.first())
                        return true
                    }
                    else -> {
                        handleMultiTripPoiMapClick()
                        return true
                    }
                }
            }
            else -> {
                handleMultiTripPoiMapClick()
                return true
            }
        }
    }

    private fun handleMultiTripPoiMapClick() {
        if (!canHandleTripPoiMapClick()) {
            return
        }
        val snackBar = Snackbar
            .make(getSnackbarView(), R.string.multiple_pois_in_this_area, Snackbar.LENGTH_SHORT)

        showNewSnackBar(snackBar)

        tripLayersManager.highlightPOI(null)
    }

    private fun handleTripPoiMapClick(tripUuid: String, poiUuid: String) {
        if (!canHandleTripPoiMapClick()) {
            return
        }
        val snackBar = Snackbar
            .make(getSnackbarView(), R.string.loading, Snackbar.LENGTH_LONG)

        showNewSnackBar(snackBar)

        loadAndShowPoiInfoAsync(tripUuid, poiUuid, snackBar)

        tripLayersManager.highlightPOI(poiUuid)
    }

    private fun loadAndShowPoiInfoAsync(tripUuid: String, poiId: String, snackBar: Snackbar) {
        val service = ServiceFactory.getTripRecordingService(this)

        lifecycleScope.launchWhenCreated {
            service.getTripRecordingByUUID(tripUuid)
                ?: throw IllegalArgumentException("Trip #$tripUuid not found.")

            val poi = service.getTripRecordingPoiByUuid(poiId)
                ?: throw IllegalArgumentException("No POI #$poiId found for trip #$tripUuid.")

            snackBar
                .setText(poi.name)
                .setAction(R.string.general_edit) {
                    onEditPoiClicked(poiId)
                }
        }
    }

    protected fun showNewSnackBar(snackBar: Snackbar?) {
        lastSnackbar?.dismiss()

        snackBar?.apply {
            animationMode = Snackbar.ANIMATION_MODE_FADE
            show()
        }

        lastSnackbar = snackBar
    }

}