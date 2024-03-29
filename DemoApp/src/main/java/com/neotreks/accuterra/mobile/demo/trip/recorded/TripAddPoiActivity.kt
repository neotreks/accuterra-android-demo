package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityTripAddPoiBinding
import com.neotreks.accuterra.mobile.demo.extensions.isNotNullNorBlank
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.demo.media.ApkMediaUtil
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.demo.util.ActivityResult
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.demo.util.PermissionSupport
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.MapLocationBuilder
import com.neotreks.accuterra.mobile.sdk.trail.model.PointType
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoiBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class TripAddPoiActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: TripAddPoiViewModel

    private lateinit var mediaListManager: TripRecordingMediaListManager

    private lateinit var binding: ActivityTripAddPoiBinding

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val TAG = "TripPoiAddActivity"

        private const val KEY_TRIP_UUID = "KEY_TRIP_UUID"
        private const val KEY_LOCATION = "KEY_LOCATION"
        private const val KEY_USER_TRIP_RECORDER = "KEY_USE_TRIP_RECORDER"

        private const val REQUEST_CAPTURE_PICTURE = 12
        private const val REQUEST_SELECT_PHOTO = 13
        private const val REQUEST_SELECT_PHOTO_PERMISSION = 14
        private const val REQUEST_TAKE_PHOTO_PERMISSIONS = 15

        fun createNavigateToIntent(
            context: Context,
            tripUuid: String,
            location: Location,
            /**
             * If true, view-model will use the [ITripRecorder]. We want to use it during the `trip recording`.
             * If false, view-model will use the [ITripRecordingService] We want to use it during the `trip editing`.
             */
            useTripRecorded: Boolean
        ): Intent {
            return Intent(context, TripAddPoiActivity::class.java)
                .apply {
                    putExtra(KEY_TRIP_UUID, tripUuid)
                    putExtra(KEY_LOCATION, location)
                    putExtra(KEY_USER_TRIP_RECORDER, useTripRecorded)
                }
        }

    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripAddPoiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this@TripAddPoiActivity).get(TripAddPoiViewModel::class.java)
        mediaListManager = TripRecordingMediaListManager(this, lifecycleScope,
            binding.activityTripAddPoiPhotos,
            object: TripRecordingMediaListManager.TripRecordingMediaListClickListener {
                override fun onItemClicked(media: TripRecordingMedia) {
                    val intent = MediaDetailActivity.createNavigateToIntent(
                        context = this@TripAddPoiActivity,
                        uri = media.uri
                    )
                    startActivity(intent)
                }
                override fun onDeleteItemClicked(media: TripRecordingMedia) {
                    viewModel.deleteMedia(media)
                }
            })

        lifecycleScope.launchWhenCreated {
            setupToolbar()
            parseIntent()
            setupTypeSpinner()
            mediaListManager.setupPhotoGrid()
            setupButtons()
            setupObservers()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAPTURE_PICTURE) {
            if (resultCode != Activity.RESULT_OK) {
                return
            }
            val path = data?.getStringExtra(CaptureImageActivity.RESULT_PATH)
                    ?: throw IllegalStateException("No image path provided for captured image.")
            viewModel.addMedia(File(path))
        }
        if (requestCode == REQUEST_SELECT_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data!!.data
                if (uri == null) {
                    longToast(getString(R.string.general_error_something_went_wrong))
                    return
                }
                viewModel.addMedia(uri, this@TripAddPoiActivity)
            }
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private suspend fun parseIntent() {
        // Parse + set the trail ID
        require(intent.hasExtra(KEY_TRIP_UUID)) { "No intent key $KEY_TRIP_UUID provided." }
        require(intent.hasExtra(KEY_LOCATION)) { "No intent key $KEY_LOCATION provided." }
        require(intent.hasExtra(KEY_USER_TRIP_RECORDER)) { "No intent key $KEY_USER_TRIP_RECORDER provided." }

        val tripUuid = intent.getStringExtra(KEY_TRIP_UUID)
        require(tripUuid.isNotNullNorBlank()) { "Invalid $KEY_TRIP_UUID value." }

        val location = intent.getParcelableExtra<Location>(KEY_LOCATION)
        requireNotNull(location) { "Invalid $KEY_LOCATION value." }

        val useTripRecorded = intent.getBooleanExtra(KEY_USER_TRIP_RECORDER, true)
        viewModel.useTripRecorder = useTripRecorded

        Log.i(TAG, "tripUUID = $tripUuid")
        viewModel.loadTrip(this, tripUuid!!, location)
    }

    private fun setupToolbar() {

        setSupportActionBar(binding.activityTripAddPoiToolbar.actionToolbar)
        binding.activityTripAddPoiToolbar.actionToolbarTitle.text = getString(R.string.activity_trip_add_poi_title)
        binding.activityTripAddPoiToolbar.actionToolbarAction.text = getString(R.string.general_save)
        binding.activityTripAddPoiToolbar.actionToolbarAction.setOnClickListener {
            onSaveClicked()
        }

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

    }

    private fun setupTypeSpinner() {
        val context = this
        lifecycleScope.launchWhenCreated {
            val service = ServiceFactory.getEnumService(context)
            val types = service.getPointTypes().sortedBy { it.name }
            val adapter = PointTypeAdapter(context, types)
            binding.activityTripAddPoiPoiType.adapter = adapter
        }
    }

    private fun setupButtons() {
        binding.activityTripAddPoiTakePhoto.setOnClickListener {
            onAddImage()
        }
        binding.activityTripAddPoiSelectPhoto.setOnClickListener {
            onSelectImage()
        }
    }

    private fun setupObservers() {
        viewModel.media.observe(this, { mediaList ->
            mediaListManager.refreshPhotoGridAdapter(mediaList)
        })
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

    private fun onSaveClicked() {

        val location = viewModel.location.value
            ?: return

        val dialog = DialogUtil.buildBlockingProgressDialog(this, getString(R.string.general_saving))
        dialog.show()

        GlobalScope.launch {

            // Gather POI data
            val name = binding.activityTripAddPoiTripName.text.toString()
            val description = binding.activityTripAddPoiTripDescription.text.toString()
            val mapLocation = MapLocationBuilder.buildFrom(location)
            val pointType = binding.activityTripAddPoiPoiType.selectedItem as PointType
            val media = viewModel.media.value!!
            val enumService = ServiceFactory.getEnumService(applicationContext)

            // Build the new POI
            val poi = TripRecordingPoiBuilder.buildNewPoi(
                name = name,
                description = description,
                mapLocation = mapLocation,
                descriptionShort = null,
                media = media, // Just default value
                pointTypeCode = pointType.code,
                enumService = enumService
            )

            // Add the newly created POI to the trip recording
            viewModel.addPoi(applicationContext, poi)

            // Close the dialog
            lifecycleScope.launchWhenCreated {
                dialog.dismiss()
                setResult(ActivityResult.RESULT_OBJECT_CREATED)
                finish()
            }

        }

    }

}
