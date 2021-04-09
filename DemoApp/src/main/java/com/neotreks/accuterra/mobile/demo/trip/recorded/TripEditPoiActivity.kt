package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityTripEditPoiBinding
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.demo.media.ApkMediaUtil
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.demo.util.ActivityResult
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.demo.util.PermissionSupport
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.PointType
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class TripEditPoiActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: TripEditPoiViewModel

    private lateinit var mediaListManager: TripRecordingMediaListManager

    private lateinit var adapter: PointTypeAdapter

    private lateinit var binding: ActivityTripEditPoiBinding

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val TAG = "TripPoiAddActivity"

        private const val KEY_POI_UUID = "KEY_POI_UUID"

        private const val REQUEST_CAPTURE_PICTURE = 12
        private const val REQUEST_SELECT_PHOTO = 13
        private const val REQUEST_SELECT_PHOTO_PERMISSION = 14
        private const val REQUEST_TAKE_PHOTO_PERMISSIONS = 15

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
        binding = ActivityTripEditPoiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this@TripEditPoiActivity).get(TripEditPoiViewModel::class.java)
        mediaListManager = TripRecordingMediaListManager(this,
            binding.activityTripEditPoiPhotos,
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

        val toolbar = binding.activityTripEditPoiToolbar
        setSupportActionBar(toolbar.actionToolbar)
        toolbar.actionToolbarTitle.text = getString(R.string.activity_trip_edit_poi_title)
        toolbar.actionToolbarAction.text = getString(R.string.general_save)
        toolbar.actionToolbarAction.setOnClickListener {
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
            adapter = PointTypeAdapter(context, types)
            binding.activityTripEditPoiPoiType.adapter = adapter
        }
    }

    private fun setupButtons() {
        binding.activityTripEditPoiSelectPhoto.setOnClickListener {
            onSelectImage()
        }
        binding.activityTripEditPoiTakePhoto.setOnClickListener {
            onAddImage()
        }
        binding.activityTripEditPoiShufflePhoto.setOnClickListener {
            onShuffleImage()
        }
    }

    private fun setupObservers() {
        viewModel.poi.observe(this, { poi ->
            binding.activityTripEditPoiTripName.setText(poi.name)
            binding.activityTripEditPoiTripDescription.setText(poi.description)
            binding.activityTripEditPoiPoiType.setSelection(adapter.getPosition(poi.pointType))
        })
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

    private fun onShuffleImage() {
        viewModel.shuffleMedia()
    }

    private fun onSaveClicked() {

        val dialog = DialogUtil.buildBlockingProgressDialog(this, getString(R.string.general_saving))
        dialog.show()

        GlobalScope.launch {

            // Gather POI data
            val name = binding.activityTripEditPoiTripName.text.toString()
            val description = binding.activityTripEditPoiTripDescription.text.toString()
            // Point types
            val pointType = binding.activityTripEditPoiPoiType.selectedItem as PointType
            // Media
            var media = viewModel.media.value!!
            media = ApkMediaUtil.updatePositions(media)

            // Build the new POI
            val poi = viewModel.poi.value!!.copy(
                name = name,
                description = description,
                descriptionShort = null,
                media = media, // Just default value
                pointType = pointType,
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
