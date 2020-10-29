package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.extensions.isNotNullNorBlank
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.demo.util.ActivityResult
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.MapLocationBuilder
import com.neotreks.accuterra.mobile.sdk.trail.model.PointSubtype
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoiBuilder
import kotlinx.android.synthetic.main.activity_trip_add_poi.*
import kotlinx.android.synthetic.main.general_action_toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class TripAddPoiActivity : AppCompatActivity() {

    private lateinit var viewModel: TripAddPoiViewModel

    private lateinit var mediaListManager: TripRecordingMediaListManager

    companion object {

        private const val TAG = "TripPoiAddActivity"

        private const val KEY_TRIP_UUID = "KEY_TRIP_UUID"
        private const val KEY_LOCATION = "KEY_LOCATION"

        private const val REQUEST_CAPTURE_PICTURE = 12
        private const val REQUEST_SELECT_PHOTO = 13

        fun createNavigateToIntent(context: Context, tripUuid: String, location: Location?): Intent {
            return Intent(context, TripAddPoiActivity::class.java)
                .apply {
                    putExtra(KEY_TRIP_UUID, tripUuid)
                    putExtra(KEY_LOCATION, location)
                }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_add_poi)
        viewModel = ViewModelProvider(this@TripAddPoiActivity).get(TripAddPoiViewModel::class.java)
        mediaListManager = TripRecordingMediaListManager(this,
            activity_trip_add_poi_photos,
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

        val tripUuid = intent.getStringExtra(KEY_TRIP_UUID)
        check(tripUuid.isNotNullNorBlank()) { "Invalid $KEY_TRIP_UUID value." }

        val location = intent.getParcelableExtra<Location>(KEY_LOCATION)
        check(location != null) { "Invalid $KEY_LOCATION value." }

        Log.i(TAG, "tripUUID = $tripUuid")
        viewModel.loadTrip(location, this)
    }

    private fun setupToolbar() {

        setSupportActionBar(action_toolbar)
        action_toolbar_title.text = getString(R.string.activity_trip_add_poi_title)
        action_toolbar_action.text = getString(R.string.general_save)
        action_toolbar_action.setOnClickListener {
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
            val subtypes = service.getPointSubtypes().sortedBy { it.name }
            val adapter = PointSubtypeAdapter(context, subtypes)
            activity_trip_add_poi_poi_type.adapter = adapter
        }
    }

    private fun setupButtons() {
        activity_trip_add_poi_take_photo.setOnClickListener {
            onAddImage()
        }
        activity_trip_add_poi_select_photo.setOnClickListener {
            onSelectImage()
        }
    }

    private fun setupObservers() {
        viewModel.media.observe(this, Observer { mediaList ->
            mediaListManager.refreshPhotoGridAdapter(mediaList)
        })
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

    private fun onSaveClicked() {

        val location = viewModel.location.value
            ?: return

        val dialog = DialogUtil.buildBlockingProgressDialog(this, getString(R.string.general_saving))
        dialog.show()

        GlobalScope.launch {

            // Gather POI data
            val name = activity_trip_add_poi_trip_name.text.toString()
            val description = activity_trip_add_poi_trip_description.text.toString()
            val isWaypoint = activity_trip_add_poi_is_wp.isChecked
            val mapLocation = MapLocationBuilder.buildFrom(location)
            val pointSubtype = activity_trip_add_poi_poi_type.selectedItem as PointSubtype
            val media = viewModel.media.value!!
            val enumService = ServiceFactory.getEnumService(applicationContext)

            // Build the new POI
            val poi = TripRecordingPoiBuilder.buildNewPoi(
                name = name,
                isWaypoint = isWaypoint,
                description = description,
                mapLocation = mapLocation,
                descriptionShort = null,
                media = media, // Just default value
                pointSubtypeCode = pointSubtype.code,
                enumService = enumService
            )

            // Add the newly created POI to the trip recording
            viewModel.addPoi(poi, applicationContext)

            // Close the dialog
            lifecycleScope.launchWhenCreated {
                dialog.dismiss()
                setResult(ActivityResult.RESULT_OBJECT_CREATED)
                finish()
            }

        }

    }

}
