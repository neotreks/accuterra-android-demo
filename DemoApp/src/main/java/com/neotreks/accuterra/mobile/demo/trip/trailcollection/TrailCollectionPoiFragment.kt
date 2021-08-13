package com.neotreks.accuterra.mobile.demo.trip.trailcollection

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.FragmentTrailCollectionPoiBinding
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.demo.media.ApkMediaUtil
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.demo.trip.recorded.CaptureImageActivity
import com.neotreks.accuterra.mobile.demo.trip.recorded.PointTypeAdapter
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripRecordingMediaListManager
import com.neotreks.accuterra.mobile.demo.ui.FormValidationError
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.demo.util.CrashSupport
import com.neotreks.accuterra.mobile.demo.util.DemoAppFileProvider
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.demo.util.PermissionSupport
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.PoiTag
import com.neotreks.accuterra.mobile.sdk.trail.model.PointType
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi
import java.io.File

class TrailCollectionPoiFragment : Fragment() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: FragmentTrailCollectionPoiBinding

    private val viewModel: TrailCollectionViewModel by activityViewModels()

    private lateinit var listener: TrailCollectionPoiFragmentListener

    private lateinit var adapter: PointTypeAdapter

    private lateinit var mediaListManager: TripRecordingMediaListManager

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "TrailCollecPoiFragment"
        private const val REQUEST_CAPTURE_PICTURE = 14
        private const val REQUEST_SELECT_PHOTO = 15
        private const val REQUEST_CAMERA_APP_PHOTO = 16

        // Permission
        private const val REQUEST_CAMERA_APP_PERMISSIONS = 17
        private const val REQUEST_TAKE_PHOTO_PERMISSIONS = 18
        private const val REQUEST_SELECT_PHOTO_PERMISSION = 19
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? TrailCollectionPoiFragmentListener
            ?: throw IllegalStateException("Fragments context is not a ${TrailCollectionPoiFragmentListener::class.qualifiedName}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrailCollectionPoiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        lifecycleScope.launchWhenCreated {
            // We need to do everything in a sequence
            setupPointTypes()
            loadTags(viewModel.selectedPointType)
            setupImageAdapter()
            setupObservers()
            // Read backup if present
            binding.fragmentTrailCollectionPoiFormTrailName.setText(viewModel.poiBackup?.name)
            binding.fragmentTrailCollectionPoiFormTrailDescription.setText(viewModel.poiBackup?.description)
            setupValueChangeListeners()
            setPointTypeChangeListener()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CAMERA_APP_PHOTO && resultCode == Activity.RESULT_OK) {
            val file = viewModel.cameraFile
            if (file == null) {
                requireActivity().longToast("Error while taking a photo")
                return
            }
            lifecycleScope.launchWhenCreated {
                val dialog = DialogUtil.buildBlockingProgressDialog(requireContext(), getString(R.string.general_saving))
                dialog.show()
                try {
                    val location = listener.getCurrentLocation()
                            ?: throw IllegalStateException("Cannot get current location")
                    viewModel.addMedia(file, location)
                    viewModel.isFormDirty = true
                    // Copy the photo also into the gallery
                    ApkMediaUtil.addPictureToTheGallery(requireContext(), file)
                } catch (e: Exception) {
                    requireActivity().longToast("Cannot read camera result due to: ${e.localizedMessage}")
                } finally {
                    dialog.dismiss()
                }
            }
        }
        if (requestCode == REQUEST_CAPTURE_PICTURE) {
            if (resultCode != Activity.RESULT_OK) {
                return
            }
            val path = data?.getStringExtra(CaptureImageActivity.RESULT_PATH)
                ?: throw IllegalStateException("No image path provided for captured image.")
            val location = listener.getCurrentLocation()
                ?: throw IllegalStateException("Cannot get current location")
            viewModel.addMedia(File(path), location)
            viewModel.isFormDirty = true
        }
        if (requestCode == REQUEST_SELECT_PHOTO) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data!!.data
                if (uri == null) {
                    activity?.longToast(getString(R.string.general_error_something_went_wrong))
                    return
                }
                val location = listener.getCurrentLocation()
                    ?: throw IllegalStateException("Cannot get current location")
                viewModel.addMedia(requireActivity(), uri, location)
                viewModel.isFormDirty = true
            }
        }

    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera App. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CAMERA_APP_PERMISSIONS -> {
                PermissionSupport.logCameraPermissionRequested(requireContext())
                if (PermissionSupport.isCameraPermissionGranted(requireContext())) {
                    onCameraAppPhoto()
                } else {
                    requireActivity().toast(getString(R.string.general_permission_not_granted))
                }
            }
            REQUEST_TAKE_PHOTO_PERMISSIONS -> {
                PermissionSupport.logCameraPermissionRequested(requireContext())
                if (PermissionSupport.isCameraPermissionGranted(requireContext())) {
                    onTakePhoto()
                } else {
                    requireActivity().toast(getString(R.string.general_permission_not_granted))
                }
            }
            REQUEST_SELECT_PHOTO_PERMISSION -> {
                PermissionSupport.logSelectPhotoPermissionRequested(requireContext())
                if (PermissionSupport.isSelectPhotoPermissionGranted(requireContext())) {
                    onSelectImage()
                } else {
                    requireActivity().longToast(getString(R.string.general_permission_not_granted))
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (listener.isAddPoiMode()) {
            backupFormData()
        }
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun setupButtons() {
        binding.fragmentTrailCollectionPoiPanelBackArrow.setOnClickListener {
            listener.onPoiPanelBackClicked()
        }
        binding.fragmentTrailCollectionPoiPanelSaveButton.setOnClickListener {
            onSavePoiClicked()
        }
        binding.fragmentTrailCollectionPoiFormSelectPhoto.setOnClickListener {
            onSelectImage()
        }
        binding.fragmentTrailCollectionPoiFormTakePhoto.setOnClickListener {
            onCameraAppPhoto()
        }
    }

    @Suppress("unused")
    private fun onCameraAppPhoto() {
        if (!PermissionSupport.isCameraPermissionGranted(requireContext())) {
            PermissionSupport.requestCameraPermissions(this, REQUEST_CAMERA_APP_PERMISSIONS)
            return
        }
        try {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val file = ApkMediaUtil.createTempCameraFile(requireContext())
            viewModel.cameraFile = file
            val photoUri = FileProvider.getUriForFile(
                requireContext(), DemoAppFileProvider.AUTHORITY, file
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(takePictureIntent, REQUEST_CAMERA_APP_PHOTO)
        } catch (e: Throwable) {
            Log.e(TAG, "Error while taking a photo", e)
            CrashSupport.reportError(e, "Error while taking a photo")
            requireActivity().longToast("Cannot take a picture: ${e.localizedMessage}")
        }
    }

    @Suppress("unused")
    private fun onTakePhoto() {
        if (!PermissionSupport.isCameraPermissionGranted(requireContext())) {
            PermissionSupport.requestCameraPermissions(this, REQUEST_TAKE_PHOTO_PERMISSIONS)
            return
        }
        val intent = CaptureImageActivity.createNavigateToIntent(this.requireContext())
        startActivityForResult(intent, REQUEST_CAPTURE_PICTURE)
    }

    private fun onSelectImage() {
        if (!PermissionSupport.isSelectPhotoPermissionGranted(requireContext())) {
            PermissionSupport.requestSelectPhotoPermission(this, REQUEST_SELECT_PHOTO_PERMISSION)
            return
        }
        val intent = ApkMediaUtil.buildSelectImageIntent()
        startActivityForResult(intent, REQUEST_SELECT_PHOTO)
    }

    private fun onSavePoiClicked() {

        // Gather POI data
        val name = binding.fragmentTrailCollectionPoiFormTrailName.text.toString()
        val description = binding.fragmentTrailCollectionPoiFormTrailDescription.text.toString()
        val pointType = binding.fragmentTrailCollectionPoiFormPoiType.selectedItem as PointType
        val tags = binding.fragmentTrailCollectionPoiFormTags.checkedChipIds.mapNotNull { id ->
            viewModel.poiTagMapping[id] ?: throw IllegalStateException("Cannot find tag object for id $id")
        }
        val media = viewModel.media.value!!

        // Validate data
        val validationError = getValidationError(name, description, tags, media)

        if (validationError == null) {
            // Everything OK -> Pass data to the listener
            listener.onSavePoi(name, description, pointType, tags, media)
        } else {
            // Display validation error dialog
            DialogUtil.buildOkDialog(
                context = requireContext(),
                title = getString(R.string.general_info),
                message = validationError.errorMessage,
                code = {
                    // Try to set focus on the problematic component
                    if (validationError.component != null) {
                        validationError.component.requestFocus()
                        binding.fragmentTrailCollectionPoiScroll.smoothScrollTo(0, validationError.component.top)
                    }
                }
            ).show()
        }

    }

    private fun backupFormData() {
        val name = binding.fragmentTrailCollectionPoiFormTrailName.text.toString()
        val description = binding.fragmentTrailCollectionPoiFormTrailDescription.text.toString()
        val pointType = binding.fragmentTrailCollectionPoiFormPoiType.selectedItem as? PointType
        val tags = binding.fragmentTrailCollectionPoiFormTags.checkedChipIds.mapNotNull { id ->
            viewModel.poiTagMapping[id] ?: throw IllegalStateException("Cannot find tag object for id $id")
        }
        viewModel.poiBackup = TrailCollectionViewModel.PoiBackup(
            name, description, pointType, tags
        )
    }

    private fun setupObservers() {
        viewModel.poi.observe(viewLifecycleOwner) { poi ->
            val isNew = poi == null
            if (isNew) {
                clearForm()
                restoreBackup()
            } else {
                loadPoi(poi!!)
            }
            loadFormHeader(isNew)
            viewModel.isFormDirty = false
        }
        viewModel.media.observe(viewLifecycleOwner, { mediaList ->
            mediaListManager.refreshPhotoGridAdapter(mediaList)
        })
    }

    private suspend fun setupPointTypes() {
        val context = this.context ?: return
        val service = ServiceFactory.getEnumService(context)
        val types = service.getPointTypes()
        viewModel.selectedPointType = types.first()
        adapter = PointTypeAdapter(context, types)
        binding.fragmentTrailCollectionPoiFormPoiType.adapter = adapter
    }

    private fun loadTags(pointType: PointType) {
        // Load all tags of `waypoint` type
        val chipGroup = binding.fragmentTrailCollectionPoiFormTags
        chipGroup.removeAllViews()
        // Let's add all tags into the list
        for((id, tag) in viewModel.poiTagMapping.entries) {
            if (tag.pointTypeCode != pointType.code) {
                continue
            }
            val chip = Chip(chipGroup.context)
            chip.id = id
            chip.text = tag.name
            chip.isCheckable = true
            chipGroup.addView(chip)
            chip.setOnCheckedChangeListener { _, _ ->
                viewModel.isFormDirty = true
            }
        }
    }

    private fun clearForm() {
        // Name and description
        binding.fragmentTrailCollectionPoiFormTrailName.text = null
        binding.fragmentTrailCollectionPoiFormTrailDescription.text = null
        // Tags
        binding.fragmentTrailCollectionPoiFormTags.clearCheck()
    }

    private fun restoreBackup() {
        // Name and description
        val backup = viewModel.poiBackup ?: return
        binding.fragmentTrailCollectionPoiFormTrailName.setText(backup.name)
        binding.fragmentTrailCollectionPoiFormTrailDescription.setText(backup.description)
        // Point type
        backup.pointType?.let { type ->
            binding.fragmentTrailCollectionPoiFormPoiType.setSelection(adapter.getPosition(type))
        }
        // Mark as selected if backup is present
        val chipGroup = binding.fragmentTrailCollectionPoiFormTags
        val mappingEntries =
            viewModel.poiBackup?.tags?.mapNotNull { tag -> viewModel.poiTagMapping.entries.find { it.value.code == tag.code } }
        if (mappingEntries != null && mappingEntries.isNotEmpty()) {
            for (entry in mappingEntries) {
                chipGroup.check(entry.key)
            }
        }
    }

    private fun setupImageAdapter() {
        mediaListManager = TripRecordingMediaListManager(requireContext(), lifecycleScope,
            binding.fragmentTrailCollectionPoiPhotos,
            object: TripRecordingMediaListManager.TripRecordingMediaListClickListener {
                override fun onItemClicked(media: TripRecordingMedia) {
                    val intent = MediaDetailActivity.createNavigateToIntent(
                        context = requireContext(),
                        uri = media.uri
                    )
                    startActivity(intent)
                }
                override fun onDeleteItemClicked(media: TripRecordingMedia) {
                    viewModel.deleteMedia(media)
                    viewModel.isFormDirty = true
                }
            })
        mediaListManager.setupPhotoGrid(columnsCount = 3)
    }

    private fun loadPoi(poi: TripRecordingPoi) {
        // Name and description
        binding.fragmentTrailCollectionPoiFormTrailName.setText(poi.name)
        binding.fragmentTrailCollectionPoiFormTrailDescription.setText(poi.description)
        // Point type
        removePointTypeChangeListener()
        binding.fragmentTrailCollectionPoiFormPoiType.setSelection(adapter.getPosition(poi.pointType), true)
        // Tags
        binding.fragmentTrailCollectionPoiFormTags.clearCheck()
        // Mark tags from POI data
        loadTags(poi.pointType)
        markTags(poi)
        // Add the listener again
        setPointTypeChangeListener()
    }

    private fun markTags(poi: TripRecordingPoi) {
        for (tag in poi.tags) {
            val entry = viewModel.poiTagMapping.entries.find { it.value.code == tag.code }
                ?: throw IllegalStateException("Cannot find tag ID for tag: $tag, mapping: ${viewModel.poiTagMapping}")
            binding.fragmentTrailCollectionPoiFormTags.check(entry.key)
        }
    }

    private fun loadFormHeader(isNew: Boolean) {
        if (isNew) {
            binding.fragmentTrailCollectionPoiHeaderText.text = getString(R.string.activity_trail_collection_poi_panel_add_new_poi)
        } else {
            binding.fragmentTrailCollectionPoiHeaderText.text = getString(R.string.activity_trail_collection_poi_panel_edit_poi)
        }
    }

    private fun setupValueChangeListeners() {
        binding.fragmentTrailCollectionPoiFormTrailName.addTextChangedListener {
            viewModel.isFormDirty = true
        }
        binding.fragmentTrailCollectionPoiFormTrailDescription.addTextChangedListener {
            viewModel.isFormDirty = true
        }
    }

    private fun removePointTypeChangeListener() {
        binding.fragmentTrailCollectionPoiFormPoiType.onItemSelectedListener = null
    }

    private fun setPointTypeChangeListener() {
        binding.fragmentTrailCollectionPoiFormPoiType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // This is an ugly workaround because we cannot detect correctly setting of the initial value
                        viewModel.isFormDirty = true
                        lifecycleScope.launchWhenCreated {
                            val selectedItem = adapter.getItem(position)
                                ?: throw java.lang.IllegalStateException("Cannot get selected point type")
                            viewModel.selectedPointType = selectedItem
                            loadTags(viewModel.selectedPointType)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        viewModel.isFormDirty = true
                    }
                }
    }


    private fun getValidationError(
        name: String,
        description: String,
        tags: List<PoiTag>,
        media: MutableList<TripRecordingMedia>
    ): FormValidationError? {
        // Check mandatory values
        if (name.length < 3) return FormValidationError( getString(R.string.activity_trail_collection_validation_name), binding.fragmentTrailCollectionPoiFormTrailName)
        if (description.length < 10) return FormValidationError( getString(R.string.activity_trail_collection_validation_description), binding.fragmentTrailCollectionPoiFormTrailDescription)
        if (media.size < 3) return FormValidationError( getString(R.string.activity_trail_collection_validation_photos), binding.fragmentTrailCollectionPoiFormTakePhoto)
        if (tags.isEmpty()) return FormValidationError(getString(R.string.activity_trail_collection_validation_tags), binding.fragmentTrailCollectionPoiFormTags)
        // Everything OK
        return null
    }


    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    interface TrailCollectionPoiFragmentListener {


        fun onPoiPanelBackClicked()

        fun onSavePoi(
            name: String,
            description: String,
            pointType: PointType,
            tags: List<PoiTag>,
            media: MutableList<TripRecordingMedia>
        )

        fun getCurrentLocation(): Location?

        fun isAddPoiMode(): Boolean

    }

}