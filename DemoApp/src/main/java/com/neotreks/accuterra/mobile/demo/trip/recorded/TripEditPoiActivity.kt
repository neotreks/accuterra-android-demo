package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.demo.util.ActivityResult
import com.neotreks.accuterra.mobile.demo.media.ApkMediaUtil
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.PointSubtype
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import kotlinx.android.synthetic.main.activity_trip_edit_poi.*
import kotlinx.android.synthetic.main.general_action_toolbar.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class TripEditPoiActivity : AppCompatActivity() {

    private lateinit var viewModel: TripEditPoiViewModel

    private lateinit var mediaListManager: TripRecordingMediaListManager

    private lateinit var adapter: PointSubtypeAdapter

    companion object {

        private const val TAG = "TripPoiAddActivity"

        private const val KEY_POI_UUID = "KEY_POI_UUID"

        private const val REQUEST_CAPTURE_PICTURE = 12
        private const val REQUEST_SELECT_PHOTO = 13

        fun createNavigateToIntent(poiUuid: String?, context: Context): Intent {
            return Intent(context, TripEditPoiActivity::class.java)
                .apply {
                    putExtra(KEY_POI_UUID, poiUuid)
                }
        }

    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_edit_poi)
        viewModel = ViewModelProvider(this@TripEditPoiActivity).get(TripEditPoiViewModel::class.java)
        mediaListManager = TripRecordingMediaListManager(this,
            activity_trip_edit_poi_photos,
            object: TripRecordingMediaListManager.TripRecordingMediaListClickListener {
                override fun onItemClicked(media: TripRecordingMedia) {
                    val intent = MediaDetailActivity.createNavigateToIntent(
                        context = this@TripEditPoiActivity,
                        mediaUuid = media.uuid
                    )
                    startActivity(intent)
                }
                override fun onDeleteItemClicked(media: TripRecordingMedia) {
                    viewModel.deleteMedia(media)
                }
            })

        lifecycleScope.launchWhenCreated {
            setupToolbar()
            setupTypeSpinner()
            mediaListManager.setupPhotoGrid()
            setupButtons()
            setupObservers()
            // Load data as the last step when all is initialized
            parseIntent()
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
                viewModel.addMedia(uri, this@TripEditPoiActivity)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        if (menu != null) {
            menuInflater.inflate(R.menu.menu_edit_poi, menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (menu != null) {
            menu.findItem(R.id.edit_poi_menu_item_delete_poi).isEnabled  = viewModel.canDeletePoi()
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.edit_poi_menu_item_delete_poi -> {
                onPoiDeleteClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private suspend fun parseIntent() {

        val poiUuid = intent.getStringExtra(KEY_POI_UUID)
        check(poiUuid != null) { "Invalid $KEY_POI_UUID value." }

        Log.i(TAG, "poiUuid = $poiUuid")
        viewModel.loadPoi(poiUuid, this)
    }

    private fun setupToolbar() {

        setSupportActionBar(action_toolbar)
        action_toolbar_title.text = getString(R.string.activity_trip_edit_poi_title)
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
            adapter = PointSubtypeAdapter(context, subtypes)
            activity_trip_edit_poi_poi_type.adapter = adapter
        }
    }

    private fun setupButtons() {
        activity_trip_edit_poi_select_photo.setOnClickListener {
            onSelectImage()
        }
        activity_trip_edit_poi_take_photo.setOnClickListener {
            onAddImage()
        }
        activity_trip_edit_poi_shuffle_photo.setOnClickListener {
            onShuffleImage()
        }
    }

    private fun setupObservers() {
        viewModel.poi.observe(this, Observer { poi ->
            activity_trip_edit_poi_trip_name.setText(poi.name)
            activity_trip_edit_poi_trip_description.setText(poi.description)
            activity_trip_edit_poi_is_wp.isChecked = poi.isWaypoint
            activity_trip_edit_poi_poi_type.setSelection(adapter.getPosition(poi.pointSubtype))
        })
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

    private fun onShuffleImage() {
        viewModel.shuffleMedia()
    }

    private fun onSaveClicked() {

        val dialog = DialogUtil.buildBlockingProgressDialog(this, getString(R.string.general_saving))
        dialog.show()

        GlobalScope.launch {

            // Gather POI data
            val name = activity_trip_edit_poi_trip_name.text.toString()
            val description = activity_trip_edit_poi_trip_description.text.toString()
            val isWaypoint = activity_trip_edit_poi_is_wp.isChecked
            // Point types
            val enumService = ServiceFactory.getEnumService(applicationContext)
            val pointSubtype = activity_trip_edit_poi_poi_type.selectedItem as PointSubtype
            val pointType = enumService.getPointTypeByCode(pointSubtype.parentTypeCode)
            // Media
            var media = viewModel.media.value!!
            media = ApkMediaUtil.updatePositions(media)

            // Build the new POI
            val poi = viewModel.poi.value!!.copy(
                name = name,
                isWaypoint = isWaypoint,
                description = description,
                descriptionShort = null,
                media = media, // Just default value
                pointType = pointType,
                pointSubtype = pointSubtype
            )

            // Add the newly created POI to the trip recording
            val recorder = ServiceFactory.getTripRecorder(applicationContext)
            recorder.updatePoi(poi)
            // Close the dialog
            lifecycleScope.launchWhenCreated {
                dialog.dismiss()
                setResult(ActivityResult.RESULT_OBJECT_UPDATED)
                finish()
            }

        }

    }

    private fun onPoiDeleteClicked() {
        DialogUtil.buildYesNoDialog(
            context = this,
            title = getString(R.string.activity_trip_edit_poi_delete_poi_title),
            message = getString(R.string.activity_trip_edit_poi_delete_poi_message),
            positiveCode = {
                // Show blocking UI
                val dialog = DialogUtil.buildBlockingProgressDialog(this, getString(R.string.general_deleting))
                dialog.show()
                // Do delete
                GlobalScope.launch {
                    val tripService = ServiceFactory.getTripRecorder(applicationContext)
                    tripService.deletePoi(viewModel.poi.value!!.uuid)
                    // Close the dialog and return
                    lifecycleScope.launchWhenCreated {
                        dialog.dismiss()
                        setResult(ActivityResult.RESULT_OBJECT_DELETED)
                        finish()
                    }
                }
            }
        ).show()
    }

}
