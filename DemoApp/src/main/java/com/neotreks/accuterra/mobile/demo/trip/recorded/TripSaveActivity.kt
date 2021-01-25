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
import com.neotreks.accuterra.mobile.demo.extensions.isNotNullNorBlank
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.demo.util.ActivityResult
import com.neotreks.accuterra.mobile.demo.media.ApkMediaUtil
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.CampingType
import com.neotreks.accuterra.mobile.sdk.trail.model.SdkCampingType
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripSharingType
import kotlinx.android.synthetic.main.activity_trip_save.*
import kotlinx.android.synthetic.main.general_toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TripSaveActivity : AppCompatActivity() {

    private lateinit var viewModel: TripSaveViewModel

    private lateinit var mediaListManager: TripRecordingMediaListManager

    companion object {

        private const val KEY_TRIP_ID = "KEY_TRIP_ID"

        private const val REQUEST_CAPTURE_PICTURE = 12
        private const val REQUEST_SELECT_PHOTO = 13

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
        setContentView(R.layout.activity_trip_save)

        viewModel = ViewModelProvider(this).get(TripSaveViewModel::class.java)
        mediaListManager = TripRecordingMediaListManager(this,
            activity_trip_save_photos,
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

    override fun onBackPressed() {
        return
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
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun populateSharingType() {
        val spinner = activity_trip_save_share
        val adapter = TripSharingTypeAdapter(this)
        spinner.adapter = adapter
    }

    private fun setupObservers() {

        viewModel.tripRecording.observe(this, { trip ->
            lifecycleScope.launch {
                activity_trip_save_trip_name.setText(trip?.tripInfo?.name)
                activity_trip_save_trip_description.setText(trip?.tripInfo?.description)
                trip?.userInfo?.userRating?.let { rating ->
                    activity_trip_save_my_rating.rating = rating
                }
                trip?.userInfo?.personalNote?.let { note ->
                    activity_trip_save_trip_personal_note.setText(note)
                }
                trip?.userInfo?.sharingType?.let { sharingType ->
                    // We expect sorting by ordinal
                    activity_trip_save_share.setSelection(sharingType.id, true)
                }
                trip?.userInfo?.promoteToTrail?.let { promote ->
                    activity_trip_save_trip_promote.isChecked = promote
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

        setSupportActionBar(general_toolbar)
        general_toolbar_title.text = getString(R.string.activity_save_trip_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(false)
            setHomeButtonEnabled(false)
        }

    }

    private fun setupButtons() {
        activity_trip_save_select_photo.setOnClickListener {
            onSelectImage()
        }
        activity_trip_save_take_photo.setOnClickListener {
            onAddImage()
        }
        activity_trip_save_shuffle_photo.setOnClickListener {
            onShuffleImage()
        }
    }

    private fun onAddImage() {
        val intent = CaptureImageActivity.createNavigateToIntent(this)
        startActivityForResult(intent, REQUEST_CAPTURE_PICTURE)
    }

    private fun onSelectImage() {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        photoPickerIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
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
            name = activity_trip_save_trip_name.text.toString(),
            description = activity_trip_save_trip_description.text.toString(),
            campingTypes = listOf(campingType)
        )
        // Add dummy data for now
        var userInfo = trip.userInfo
        val rating = activity_trip_save_my_rating.rating
        val note = activity_trip_save_trip_personal_note.text?.toString()
        val sharingType = activity_trip_save_share.selectedItem as TripSharingType
        userInfo = userInfo.copy(
            userRating = rating,
            personalNote = note,
            sharingType = sharingType,
            promoteToTrail = activity_trip_save_trip_promote.isChecked
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
