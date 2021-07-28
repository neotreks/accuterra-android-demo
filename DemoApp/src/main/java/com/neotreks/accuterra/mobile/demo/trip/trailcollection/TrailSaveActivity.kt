package com.neotreks.accuterra.mobile.demo.trip.trailcollection

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.slider.Slider
import com.neotreks.accuterra.mobile.demo.*
import com.neotreks.accuterra.mobile.demo.databinding.ActivityTrailSaveBinding
import com.neotreks.accuterra.mobile.demo.extensions.fromInchesToMeters
import com.neotreks.accuterra.mobile.demo.extensions.fromMetersToInches
import com.neotreks.accuterra.mobile.demo.extensions.isNotNullNorBlank
import com.neotreks.accuterra.mobile.demo.extensions.trimOrNull
import com.neotreks.accuterra.mobile.demo.media.ApkMediaUtil
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripRecordingMediaFileViewAdapter
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripRecordingMediaListManager
import com.neotreks.accuterra.mobile.demo.ui.FormValidationError
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.demo.ui.SeasonRecommendationInputFilter
import com.neotreks.accuterra.mobile.demo.util.*
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.model.ExtPropertiesBuilder
import com.neotreks.accuterra.mobile.sdk.trail.model.*
import com.neotreks.accuterra.mobile.sdk.trail.service.ITrailMediaService
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecording
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingPoi
import com.neotreks.accuterra.mobile.sdk.trip.model.TripSharingType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TrailSaveActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: TrailSaveViewModel

    private lateinit var mediaListManager: TripRecordingMediaListManager

    private lateinit var binding: ActivityTrailSaveBinding

    private lateinit var poiImageAdapter: TripRecordingMediaFileViewAdapter

    private lateinit var mediaService: ITrailMediaService

    private var accessConcernsMapping: Map<Int, AccessConcern> = mapOf()

    private var isFormListenersInitialized = false

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val KEY_TRIP_ID = "KEY_TRIP_ID"

        private const val REQUEST_SELECT_PHOTO = 13
        private const val REQUEST_SELECT_PHOTO_PERMISSION = 14

        fun createNavigateToIntent(context: Context, tripUUID: String): Intent {
            return Intent(context, TrailSaveActivity::class.java)
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
        binding = ActivityTrailSaveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(TrailSaveViewModel::class.java)
        mediaService = ServiceFactory.getTrailMediaService(this)
        mediaListManager = TripRecordingMediaListManager(this, lifecycleScope,
            binding.activityTrailSavePhotos,
            object: TripRecordingMediaListManager.TripRecordingMediaListClickListener {
                override fun onItemClicked(media: TripRecordingMedia) {
                    val intent = MediaDetailActivity.createNavigateToIntent(this@TrailSaveActivity,
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
        mediaListManager.setupPhotoGrid()
        setupForm()

        setupObservers()

        parseIntent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menu?.let {
            menuInflater.inflate(R.menu.menu_trail_save, menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (menu != null) {
            val canEdit = viewModel.canEditTrip()
            menu.findItem(R.id.trail_save_menu_item_trail_save_and_upload).isEnabled  = canEdit
            menu.findItem(R.id.trail_save_menu_item_trail_save).isEnabled  = canEdit
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.trail_save_menu_item_trail_save -> {
                onSaveClicked(false)
                return true
            }
            R.id.trail_save_menu_item_trail_save_and_upload -> {
                onSaveClicked(true)
                return true
            }
            R.id.trail_save_menu_item_trail_delete -> {
                onDeleteClicked()
                return true
            }
            R.id.trail_save_menu_item_close_no_save -> {
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
        if (viewModel.isFormDirty) {
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
        } else {
            super.onBackPressed()
        }
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun setupObservers() {

        viewModel.tripRecording.observe(this, { trip ->
            // Refresh the menu
            invalidateOptionsMenu()
            if (trip == null) {
                return@observe
            }
            lifecycleScope.launch {
                loadTrailToForm(trip, viewModel.tripPois)
            }
        })

        viewModel.poiMedia.observe(this@TrailSaveActivity, {
            showImages()
        })

        viewModel.tripMedia.observe(this, { mediaList ->
            mediaListManager.refreshPhotoGridAdapter(mediaList)
        })

    }

    private fun loadTrailToForm(trip: TripRecording, pois: List<TripRecordingPoi>) {
        // Load access concern spinner
        loadAccessConcerns()
        loadTags(trip, pois)
        // Bind values
        binding.activityTrailSaveTrailName.setText(trip.tripInfo.name)
        binding.activityTrailSaveTrailDescription.setText(trip.tripInfo.description)
        trip.extProperties.firstOrNull()?.data?.jsonToClass(TrailCollectionData::class.java)?.let { trailCollectionData ->
            // Main Custom data
            // For Trail collection we define just one value
            val techRatingLow = trailCollectionData.difficultyRating?.toFloat() ?: 1f
            binding.activityTrailSaveTechRating.value = techRatingLow
            binding.activityTrailSaveTechRatingText.text = viewModel.techRatings.find { it.level == techRatingLow.toInt() }?.name
            binding.activityTrailSaveTrailDifficultyDescription.setText(trailCollectionData.difficultyDescription)
            // Optional: Basic
            binding.activityTrailSaveHighlights.setText(trailCollectionData.highlights)
            binding.activityTrailSaveHistory.setText(trailCollectionData.history)
            binding.activityTrailSaveCampingOptions.setText(trailCollectionData.campingOptions)
            // Optional: Permit
            val permitRequired = trailCollectionData.permitRequired
            binding.activityTrailSavePermitRequired.isChecked = permitRequired
            binding.activityTrailSavePermitContainer.visibility = permitRequired.visibility
            binding.activityTrailSavePermitInfo.setText(trailCollectionData.permitInformation)
            binding.activityTrailSavePermitInfoLink.setText(trailCollectionData.permitInformationLink)
            binding.activityTrailSaveAccessIssue.setText(trailCollectionData.accessIssue)
            binding.activityTrailSaveAccessIssueLink.setText(trailCollectionData.accessIssueLink)
            // Optional: Season
            binding.activityTrailSaveSeasonRecommendation.setText(trailCollectionData.seasonalityRecommendation)
            binding.activityTrailSaveSeasonRecommendationReason.setText(trailCollectionData.seasonalityRecommendationReason)
            binding.activityTrailSaveSeasonSpring.setText(trailCollectionData.seasonSpring)
            binding.activityTrailSaveSeasonSummer.setText(trailCollectionData.seasonSummer)
            binding.activityTrailSaveSeasonFall.setText(trailCollectionData.seasonFall)
            binding.activityTrailSaveSeasonWinter.setText(trailCollectionData.seasonWinter)
            // Optional: Other
            for (concernCode in trailCollectionData.accessConcernCodes) {
                val entry  = accessConcernsMapping.entries.find { it.value.code == concernCode }
                    ?: throw IllegalStateException("Cannot find concern code: $concernCode, mapping: $accessConcernsMapping")
                binding.activityTrailSaveTrailAccessConcerns.check(entry.key)
            }
            binding.activityTrailSaveTrailRecommendedClearance.setText(trailCollectionData.recommendedClearance?.fromMetersToInches()?.toString())
            binding.activityTrailSaveTrailBestDirection.setText(trailCollectionData.bestDirection)
            binding.activityTrailSaveTrailPersonalNote.setText(trip.userInfo.personalNote)
            // Preferred image
            viewModel.preferredImageUuid = trailCollectionData.preferredImageUuid
        }

        if (!isFormListenersInitialized) {
            initializeFormListeners()
        }
    }

    /**
     * Initialize listeners checking if for has been modified.
     */
    private fun initializeFormListeners() {
        binding.activityTrailSaveTrailName.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveTrailDescription.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveTechRating.addOnChangeListener(Slider.OnChangeListener { _, _, _ -> onFormValueChanged() })
        binding.activityTrailSaveTrailDifficultyDescription.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveHighlights.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveHistory.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveCampingOptions.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSavePermitRequired.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSavePermitInfo.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSavePermitInfoLink.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveAccessIssue.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveAccessIssueLink.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveSeasonRecommendation.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveSeasonRecommendationReason.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveSeasonSpring.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveSeasonSummer.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveSeasonFall.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveSeasonWinter.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveTrailRecommendedClearance.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveTrailBestDirection.addTextChangedListener { onFormValueChanged() }
        binding.activityTrailSaveTrailPersonalNote.addTextChangedListener { onFormValueChanged() }
    }

    private fun onFormValueChanged() {
        viewModel.isFormDirty = true
    }

    private fun loadTags(trip: TripRecording, pois: List<TripRecordingPoi>) {
        viewModel.tripRecording
        val tripTags = trip.tripInfo.tags
        val poiTags = pois.flatMap { it.tags }.distinctBy { it.code }
        val chipGroup = binding.activityTrailSaveTags
        // Clear previous values
        chipGroup.removeAllViews()
        // Let's add all tags into the list
        for((id, tag) in viewModel.tagMapping.entries) {
            // Avoid navigation tags
            if (tag.pointTypeCode == SdkPointType.NAVIGATION.code) {
                continue
            }
            val isPoiTag = poiTags.contains(tag)
            val isSelected = tripTags.contains(tag)
            // Handle UI
            val chip = Chip(chipGroup.context)
            chip.id = id
            chip.text = tag.name
            // Mark as checked if it is a trip tag
            chip.isCheckable = true
            chip.isChecked = isSelected
            // We allow to change only non-poi tags
            chip.isClickable = !isPoiTag
            chipGroup.addView(chip)
            // We want to handle only click on non-poi tags
            if (!isPoiTag) {
                chip.setOnCheckedChangeListener { _, _ ->
                    viewModel.isFormDirty = true
                }
            }
        }
    }

    private fun parseIntent() {
        require(intent.hasExtra(KEY_TRIP_ID)) { "No intent key $KEY_TRIP_ID provided." }

        val tripId = intent.getStringExtra(KEY_TRIP_ID)
        require(tripId.isNotNullNorBlank()) { "Invalid $KEY_TRIP_ID value." }

        viewModel.loadTrip(tripId!!, this)
    }

    private fun setupToolbar() {

        setSupportActionBar(binding.activityTrailSaveToolbar.trailCollectionToolbar)
        binding.activityTrailSaveToolbar.trailCollectionToolbarTitle.text = getString(R.string.activity_save_trail_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

    }

    private fun setupButtons() {
        binding.activityTrailSaveSelectPhoto.setOnClickListener {
            onSelectImage()
        }
    }

    private fun setupForm() {
        // Show more/less button
        binding.activityTrailSaveShowMoreLabel.setOnClickListener {
            val isMoreVisible: Boolean = !binding.activityTrailSaveOptionalFieldsContainer.isVisible
            binding.activityTrailSaveOptionalFieldsContainer.visibility = (isMoreVisible).visibility
            if (isMoreVisible) {
                binding.activityTrailSaveShowMoreLabel.text = getString(R.string.general_show_less)
                // A workaround to move the form lower
                binding.activityTrailSaveHighlights.requestFocus()
                binding.activityTrailSaveHighlights.clearFocus()
            } else {
                binding.activityTrailSaveShowMoreLabel.text = getString(R.string.general_show_more)
            }
        }
        // Hide optional fields at the beginning
        binding.activityTrailSaveOptionalFieldsContainer.visibility = false.visibility
        // Display names of selected technical ratings
        binding.activityTrailSaveTechRating.addOnChangeListener(Slider.OnChangeListener { slider, _, _ ->
            lifecycleScope.launchWhenCreated {
                val techRatings = viewModel.techRatings
                val value = slider.value
                binding.activityTrailSaveTechRatingText.text = techRatings.find { it.level == value.toInt() }?.name
            }
        })
        // handle the permit required switch
        binding.activityTrailSavePermitRequired.setOnCheckedChangeListener { _, isChecked ->
            binding.activityTrailSavePermitContainer.visibility = isChecked.visibility
        }
        // Season recommendation filter
        binding.activityTrailSaveSeasonRecommendation.filters = arrayOf(SeasonRecommendationInputFilter())
    }

    private fun loadAccessConcerns() {
        // Load all concerns
        val accessConcerns = viewModel.accessConcerns
        val chipGroup = binding.activityTrailSaveTrailAccessConcerns
        chipGroup.removeAllViews()
        // Let's add all tags into the list
        val mapping = mutableMapOf<Int, AccessConcern>()
        accessConcerns.forEachIndexed { id, accessConcern ->
            val chip = Chip(chipGroup.context)
            chip.id = id
            chip.text = accessConcern.name
            chip.isCheckable = true
            chipGroup.addView(chip)
            mapping[id] = accessConcern
            chip.setOnCheckedChangeListener { _, _ ->
                viewModel.isFormDirty = true
            }
        }
        this.accessConcernsMapping = mapping
    }

    private fun onSelectImage() {
        if (!PermissionSupport.isSelectPhotoPermissionGranted(this)) {
            PermissionSupport.requestSelectPhotoPermission(this, REQUEST_SELECT_PHOTO_PERMISSION)
            return
        }
        val photoPickerIntent = ApkMediaUtil.buildSelectImageIntent()
        startActivityForResult(photoPickerIntent, REQUEST_SELECT_PHOTO)
    }

    private fun onSaveClicked(doUpload: Boolean) {

        // Load one fo the camping types
        lateinit var campingType: CampingType
        runBlocking {
            val enumService = ServiceFactory.getEnumService(this@TrailSaveActivity)
            campingType = enumService.getCampingTypeByCode(SdkCampingType.DISPERSED.code)
        }

        var trip = viewModel.tripRecording.value
            ?: throw IllegalStateException("Trip not set")

        // UI blocking dialog
        val savingDialog = DialogUtil.buildBlockingProgressDialog(this, getString(R.string.general_saving))
        savingDialog.show()

        // Tags
        val selectedTagIds = binding.activityTrailSaveTags.checkedChipIds
        val tags = viewModel.tagMapping.filter { selectedTagIds.contains(it.key) }.values.toList()

        // Update the trip with gathered data
        var tripInfo = trip.tripInfo
        tripInfo = tripInfo.copy(
            name = binding.activityTrailSaveTrailName.text.toString(),
            description = binding.activityTrailSaveTrailDescription.text.trimOrNull(),
            campingTypes = listOf(campingType),
            tags = tags
        )
        // Add dummy data for now
        var userInfo = trip.userInfo
        val rating = null
        val note = binding.activityTrailSaveTrailPersonalNote.text.trimOrNull()
        val sharingType = TripSharingType.PRIVATE
        val promote = true
        userInfo = userInfo.copy(
            userRating = rating,
            personalNote = note,
            sharingType = sharingType,
            promoteToTrail = promote
        )
        // Custom Data
        val ratingValue = binding.activityTrailSaveTechRating.value.toInt()
        val permitRequired = binding.activityTrailSavePermitRequired.isChecked
        val trailCollectionData = TrailCollectionData(
            // Mandatory
            // For Trail collection we gather just one value and not low/high values
            difficultyRating = ratingValue,
            difficultyDescription = binding.activityTrailSaveTrailDifficultyDescription.text.trimOrNull(),
            // Optional: Basic
            highlights = binding.activityTrailSaveHighlights.text.trimOrNull(),
            history = binding.activityTrailSaveHistory.text.trimOrNull(),
            campingOptions = binding.activityTrailSaveCampingOptions.text.trimOrNull(),
            // Optional: Permit
            permitRequired = permitRequired,
            permitInformation = if (permitRequired) binding.activityTrailSavePermitInfo.text.trimOrNull() else null,
            permitInformationLink = if (permitRequired) binding.activityTrailSavePermitInfoLink.text.trimOrNull() else null,
            accessIssue = if (permitRequired) binding.activityTrailSaveAccessIssue.text.trimOrNull() else null,
            accessIssueLink = if (permitRequired) binding.activityTrailSaveAccessIssueLink.text.trimOrNull() else null,
            // Optional: Season
            seasonalityRecommendation = binding.activityTrailSaveSeasonRecommendation.text.trimOrNull(),
            seasonalityRecommendationReason = binding.activityTrailSaveSeasonRecommendationReason.text.trimOrNull(),
            seasonSpring = binding.activityTrailSaveSeasonSpring.text.trimOrNull(),
            seasonSummer = binding.activityTrailSaveSeasonSummer.text.trimOrNull(),
            seasonFall = binding.activityTrailSaveSeasonFall.text.trimOrNull(),
            seasonWinter = binding.activityTrailSaveSeasonWinter.text.trimOrNull(),
            // Optional: Other
            accessConcernCodes = binding.activityTrailSaveTrailAccessConcerns.checkedChipIds.map { id ->
                accessConcernsMapping[id]?.code ?: throw IllegalStateException("Cannot find concern for ID; $id, mapping: $accessConcernsMapping")
            },
            recommendedClearance = binding.activityTrailSaveTrailRecommendedClearance.text.toString().toFloatOrNull()?.fromInchesToMeters(),
            bestDirection = binding.activityTrailSaveTrailBestDirection.text.trimOrNull(),
            preferredImageUuid = viewModel.preferredImageUuid
        )

        // Media
        var allMedia = viewModel.tripMedia.value!!
        allMedia = ApkMediaUtil.updatePositions(allMedia)

        // Copy gathered data
        trip = trip.copy(
            tripInfo = tripInfo,
            userInfo = userInfo,
            media = allMedia,
            extProperties = ExtPropertiesBuilder.buildList(trailCollectionData),
        )

        // Validation
        val validationError = getValidationError(trip)
        if (validationError != null) {
            savingDialog.dismiss()
            // Display validation error dialog
            DialogUtil.buildOkDialog(
                context = this,
                title = getString(R.string.general_info),
                message = validationError.errorMessage,
                code = {
                    // Try to set focus on the problematic component
                    if (validationError.component != null) {
                        validationError.component.requestFocus()
                    }
                }
            ).show()
            // Do not continue
            return
        }

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
                savingDialog.dismiss()
                toast(getString(R.string.activity_save_trail_trail_saved))
                setResult(RESULT_OK)
                finish()
            }
        }

    }

    private fun showImages() {
        // POI images
        poiImageAdapter = TripRecordingMediaFileViewAdapter(
            this,
            lifecycleScope,
            (viewModel.poiMedia.value ?: listOf()).toTypedArray(),
            true
        )
        binding.activityTrailSaveImageCarousel.adapter = poiImageAdapter
        loadPreferredImage()
        // Trip images
        poiImageAdapter.setClickListener(object: TripRecordingMediaFileViewAdapter.ItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                val item = poiImageAdapter.getItem(position)
                onPreferredImageClicked(item, position)
            }
            override fun onDeleteItemClick(view: View?, position: Int) {}
        })
    }

    private fun onPreferredImageClicked(media: TripRecordingMedia, position: Int) {
        if (viewModel.preferredImageUuid == media.uuid) {
            // Deselect image as preferred
            viewModel.preferredImageUuid = null
            poiImageAdapter.setFavoritePosition(null)
        } else {
            // Select image as preferred
            viewModel.preferredImageUuid = media.uuid
            poiImageAdapter.setFavoritePosition(position)
        }
    }

    /**
     * Loads preferred image into the UI
     */
    private fun loadPreferredImage() {
        val uuid = viewModel.preferredImageUuid
        if (uuid == null) {
            poiImageAdapter.setFavoritePosition(null)
        } else {
            val position = poiImageAdapter.getPositionByUuid(uuid)
            poiImageAdapter.setFavoritePosition(position)
        }
    }

    private fun onDeleteClicked() {
        DialogUtil.buildYesNoDialog(
            context = this,
            title = getString(R.string.activity_save_trail_delete_trail_title),
            message = getString(R.string.activity_save_trail_delete_trail_message),
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

    /**
     * Validate trail form data
     */
    private fun getValidationError(trip: TripRecording): FormValidationError? {
        if (trip.tripInfo.name.length < 3) {
            return FormValidationError(getString(R.string.activity_save_trail_validation_name),
                binding.activityTrailSaveTrailName)
        }
        if ((trip.tripInfo.description?.length ?: 0) < 10) {
            return FormValidationError(getString(R.string.activity_save_trail_validation_description),
                binding.activityTrailSaveTrailDescription)
        }

        val trailCollectionData =
            trip.extProperties.firstOrNull()?.data?.jsonToClass(TrailCollectionData::class.java)
                ?: return FormValidationError(getString(R.string.activity_save_trail_validation_trail_collection_data))

        if (trailCollectionData.difficultyDescription?.length ?: 0 < 10) {
            return FormValidationError(
                getString(R.string.activity_save_trail_validation_description),
                binding.activityTrailSaveTrailDifficultyDescription
            )
        }

        return null
    }

}
