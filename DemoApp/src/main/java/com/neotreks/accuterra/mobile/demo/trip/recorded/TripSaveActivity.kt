package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityTripSaveBinding
import com.neotreks.accuterra.mobile.demo.extensions.isNotNullNorBlank
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.demo.util.ActivityResult
import com.neotreks.accuterra.mobile.demo.media.ApkMediaUtil
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.demo.util.PermissionSupport
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.CampingType
import com.neotreks.accuterra.mobile.sdk.trail.model.SdkCampingType
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripSharingType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TripSaveActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: TripSaveViewModel

    private lateinit var mediaListManager: TripRecordingMediaListManager

    private lateinit var binding: ActivityTripSaveBinding

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val KEY_TRIP_ID = "KEY_TRIP_ID"

        private const val REQUEST_CAPTURE_PICTURE = 12
        private const val REQUEST_SELECT_PHOTO = 13
        private const val REQUEST_SELECT_PHOTO_PERMISSION = 14
        private const val REQUEST_TAKE_PHOTO_PERMISSIONS = 15

        fun createNavigateToIntent(context: Context, tripUUID: String): Intent {
            return Intent(context, TripSaveActivity::class.java)
                .apply {
                    putExtra(KEY_TRIP_ID, tripUUID)
                }
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(TripSaveViewModel::class.java)
        mediaListManager = TripRecordingMediaListManager(this,
            binding.activityTripSavePhotos,
            object: TripRecordingMediaListManager.TripRecordingMediaListClickListener {
                override fun onItemClicked(media: TripRecordingMedia) {
                    val intent = MediaDetailActivity.createNavigateToIntent(this@TripSaveActivity,
                        mediaUuid = media.uuid,
                        uri = media.uri
                    )
                    startActivity(intent)
                }
                override fun onDeleteItemClicked(media: TripRecordingMedia) {
                    viewModel.deleteMedia(media)
                }
            })

        setupToolbar()
        setupButtons()
        populateSharingType()
        mediaListManager.setupPhotoGrid()

        setupObservers()

        parseIntent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAPTURE_PICTURE) {
            if (resultCode != Activity.RESULT_OK) {
                return
            }
            val path = data?.getStringExtra(CaptureImageActivity.RESULT_PATH)
                ?: throw IllegalStateException("No image path provided for captured image.")
            viewModel.addMedia(path)
        }
        if (requestCode == REQUEST_SELECT_PHOTO && resultCode == Activity.RESULT_OK) {
            val uri = data!!.data
            if (uri == null) {
                longToast(getString(R.string.general_error_something_went_wrong))
                return
            }
            viewModel.addMedia(uri, this)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_SELECT_PHOTO_PERMISSION) {
            PermissionSupport.logSelectPhotoPermissionRequested(this)
            if (PermissionSupport.isSelectPhotoPermissionGranted(this)) {
                onSelectImage()
            } else {
                longToast(getString(R.string.general_permission_not_granted))
            }
        }
        if (requestCode == REQUEST_TAKE_PHOTO_PERMISSIONS) {
            PermissionSupport.logCameraPermissionRequested(this)
            if (PermissionSupport.isCameraPermissionGranted(this)) {
                onAddImage()
            } else {
                longToast(getString(R.string.general_permission_not_granted))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menu?.let {
            menuInflater.inflate(R.menu.menu_trip_save, menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (menu != null) {
            val canEdit = viewModel.canEditTrip()
            menu.findItem(R.id.trip_save_menu_item_trip_save_and_upload).isEnabled  = canEdit
            menu.findItem(R.id.trip_save_menu_item_trip_save).isEnabled  = canEdit
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.trip_save_menu_item_trip_save -> {
                onSaveClicked(false)
                return true
            }
            R.id.trip_save_menu_item_trip_save_and_upload -> {
                onSaveClicked(true)
                return true
            }
            R.id.trip_save_menu_item_trip_delete -> {
                onDeleteClicked()
                return true
            }
            R.id.trip_save_menu_item_close_no_save -> {
                finish()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        DialogUtil.buildYesNoDialog(
            context = this,
            title = getString(R.string.general_close_without_saving_title),
            message = getString(R.string.general_close_without_saving_message),
            positiveCodeLabel = getString(R.string.general_close_without_saving_no_stay),
            negativeCodeLabel = getString(R.string.general_close_without_saving_yes_exit),
            negativeCode = {
                super.onBackPressed()
            }
        ).show()
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun populateSharingType() {
        val spinner = binding.activityTripSaveShare
        val adapter = TripSharingTypeAdapter(this)
        spinner.adapter = adapter
    }

    private fun setupObservers() {

        viewModel.tripRecording.observe(this, { trip ->
            lifecycleScope.launch {
                binding.activityTripSaveTripName.setText(trip?.tripInfo?.name)
                binding.activityTripSaveTripDescription.setText(trip?.tripInfo?.description)
                trip?.userInfo?.userRating?.let { rating ->
                    binding.activityTripSaveMyRating.rating = rating
                }
                trip?.userInfo?.personalNote?.let { note ->
                    binding.activityTripSaveTripPersonalNote.setText(note)
                }
                trip?.userInfo?.sharingType?.let { sharingType ->
                    // We expect sorting by ordinal
                    binding.activityTripSaveShare.setSelection(sharingType.ordinal, true)
                }
                trip?.userInfo?.promoteToTrail?.let { promote ->
                    binding.activityTripSaveTripPromote.isChecked = promote
                }
                // Refresh the menu
                invalidateOptionsMenu()
            }
        })

        viewModel.media.observe(this, { mediaList ->
            mediaListManager.refreshPhotoGridAdapter(mediaList)
        })

    }

    private fun parseIntent() {
        require(intent.hasExtra(KEY_TRIP_ID)) { "No intent key $KEY_TRIP_ID provided." }

        val tripId = intent.getStringExtra(KEY_TRIP_ID)
        require(tripId.isNotNullNorBlank()) { "Invalid $KEY_TRIP_ID value." }

        viewModel.loadTrip(tripId!!, this)
    }

    private fun setupToolbar() {

        setSupportActionBar(binding.activityTripSaveToolbar.generalToolbar)
        binding.activityTripSaveToolbar.generalToolbarTitle.text = getString(R.string.activity_save_trip_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

    }

    private fun setupButtons() {
        binding.activityTripSaveSelectPhoto.setOnClickListener {
            onSelectImage()
        }
        binding.activityTripSaveTakePhoto.setOnClickListener {
            onAddImage()
        }
        binding.activityTripSaveShufflePhoto.setOnClickListener {
            onShuffleImage()
        }
    }

    private fun onAddImage() {
        if (!PermissionSupport.isCameraPermissionGranted(this)) {
            PermissionSupport.requestCameraPermissions(this, REQUEST_TAKE_PHOTO_PERMISSIONS)
            return
        }
        val intent = CaptureImageActivity.createNavigateToIntent(this)
        startActivityForResult(intent, REQUEST_CAPTURE_PICTURE)
    }

    private fun onSelectImage() {
        if (!PermissionSupport.isSelectPhotoPermissionGranted(this)) {
            PermissionSupport.requestSelectPhotoPermission(this, REQUEST_SELECT_PHOTO_PERMISSION)
            return
        }
        val photoPickerIntent = ApkMediaUtil.buildSelectImageIntent()
        startActivityForResult(photoPickerIntent, REQUEST_SELECT_PHOTO)
    }

    private fun onShuffleImage() {
        viewModel.shuffleMedia()
    }

    private fun onSaveClicked(doUpload: Boolean) {

        // Load one fo the camping types
        lateinit var campingType: CampingType
        runBlocking {
            val enumService = ServiceFactory.getEnumService(this@TripSaveActivity)
            campingType = enumService.getCampingTypeByCode(SdkCampingType.DISPERSED.code)
        }

        var trip = viewModel.tripRecording.value
            ?: throw IllegalStateException("Trip not set")

        // UI blocking dialog
        val dialog = DialogUtil.buildBlockingProgressDialog(this, getString(R.string.general_saving))
        dialog.show()

        // Update the trip with gathered data
        var tripInfo = trip.tripInfo
        tripInfo = tripInfo.copy(
            name = binding.activityTripSaveTripName.text.toString(),
            description = binding.activityTripSaveTripDescription.text.toString(),
            campingTypes = listOf(campingType)
        )
        // Add dummy data for now
        var userInfo = trip.userInfo
        val rating = binding.activityTripSaveMyRating.rating
        val note = binding.activityTripSaveTripPersonalNote.text?.toString()
        val sharingType = binding.activityTripSaveShare.selectedItem as TripSharingType
        userInfo = userInfo.copy(
            userRating = rating,
            personalNote = note,
            sharingType = sharingType,
            promoteToTrail = binding.activityTripSaveTripPromote.isChecked
        )
        // Media
        var allMedia = viewModel.media.value!!
        allMedia = ApkMediaUtil.updatePositions(allMedia)
        // Copy gathered data
        trip = trip.copy(
            tripInfo = tripInfo,
            userInfo = userInfo,
            media = allMedia
        )

        GlobalScope.launch {
            // Safe Trip Data into the DB
            val tripService = ServiceFactory.getTripRecordingService(applicationContext)
            tripService.updateTripRecording(trip)
            // Trigger upload the recorded trip to the server
            if (doUpload) {
                tripService.uploadTripRecordingToServer(trip.tripInfo.uuid)
            }
            // Do UI stuff
            lifecycleScope.launchWhenCreated {
                dialog.dismiss()
                toast(getString(R.string.activity_save_trip_trip_saved))
                setResult(RESULT_OK)
                finish()
            }
        }

    }

    private fun onDeleteClicked() {
        DialogUtil.buildYesNoDialog(
            context = this,
            title = getString(R.string.activity_save_trip_delete_trip_title),
            message = getString(R.string.activity_save_trip_delete_trip_message),
            positiveCode = {
                deleteTrip()
            }
        ).show()

    }

    private fun deleteTrip() {

        val tripUuid = viewModel.tripRecording.value?.tripInfo?.uuid
            ?: throw IllegalStateException("Cannot get the trip UUID")

        val dialog = DialogUtil.buildBlockingProgressDialog(this, getString(R.string.general_deleting))
        dialog.show()

        GlobalScope.launch {

            // It is recommended to use the [ITripService.deleteTrip] method.
            // In case the trip is actually recoded, it is possible to use
            // also the [ITripRecorder.deleteTrip] method.
            val service = ServiceFactory.getTripRecordingService(applicationContext)
            service.deleteTripRecording(tripUuid)

            // Close this activity
            lifecycleScope.launchWhenCreated {
                dialog.dismiss()
                setResult(ActivityResult.RESULT_OBJECT_DELETED)
                finish()
            }
        }

    }

}
