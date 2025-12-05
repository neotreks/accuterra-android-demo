package com.neotreks.accuterra.mobile.demo

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.neotreks.accuterra.mobile.demo.databinding.ActivityTrailInfoBinding
import com.neotreks.accuterra.mobile.demo.extensions.getFavoriteIconResource
import com.neotreks.accuterra.mobile.demo.offline.ApkOfflineCacheBackgroundService
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.demo.settings.UserSettingsActivity
import com.neotreks.accuterra.mobile.demo.trip.location.LocationActivity
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.demo.ui.ProgressDialogHolder
import com.neotreks.accuterra.mobile.demo.util.CrashSupport
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.demo.util.EnumUtil
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.cache.OfflineCacheBackgroundService
import com.neotreks.accuterra.mobile.sdk.location.SdkLocationProvider
import com.neotreks.accuterra.mobile.sdk.map.cache.*
import com.neotreks.accuterra.mobile.sdk.map.navigation.TrailNavigator
import com.neotreks.accuterra.mobile.sdk.trail.model.Trail
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailMedia
import com.neotreks.accuterra.mobile.sdk.trail.service.ITrailMediaService
import com.neotreks.accuterra.mobile.sdk.ugc.model.PostTrailCommentRequest
import com.neotreks.accuterra.mobile.sdk.ugc.model.PostTrailCommentRequestBuilder
import com.neotreks.accuterra.mobile.sdk.ugc.model.TrailComment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import kotlin.math.round


class TrailInfoActivity : LocationActivity(), TrailMediaClickListener {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val TAG = "TrailInfoActivity"

        private const val KEY_TRAIL_ID = "TRAIL_ID"

        fun createNavigateToIntent(context: Context, trailId: Long): Intent {
            return Intent(context, TrailInfoActivity::class.java)
                .apply {
                    putExtra(KEY_TRAIL_ID, trailId)
                }
        }
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: TrailInfoViewModel

    private lateinit var binding: ActivityTrailInfoBinding

    private var dialogHolder = ProgressDialogHolder()

    private lateinit var imageAdapter: TrailMediaAdapter

    private lateinit var mediaService: ITrailMediaService

    private var offlineCacheBgService: OfflineCacheBackgroundService? = null

    private val cacheProgressListener = CacheProgressListener(this)

    private val discussionListener = UgcListener(this)

    private val connectionListener = object: OfflineCacheServiceConnectionListener {
        override fun onConnected(service: OfflineCacheBackgroundService) {
            offlineCacheBgService = service
            lifecycleScope.launchWhenCreated {
                offlineCacheBgService?.offlineMapManager?.addProgressListener(cacheProgressListener)
                configureDownloadButton(service.offlineMapManager.getTrailOfflineMap(
                    viewModel.trailId)?.status ?: OfflineMapStatus.NOT_CACHED)
            }
        }

        override fun onDisconnected() {
            offlineCacheBgService?.offlineMapManager?.removeProgressListener(cacheProgressListener)
            offlineCacheBgService = null
            Log.d(TAG, "offlineCacheService disconnected")
        }
    }

    private val offlineCacheServiceConnection = OfflineCacheBackgroundService.createServiceConnection(
        connectionListener
    )

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onStart() {
        super.onStart()
        ApkOfflineCacheBackgroundService.bindService(this, lifecycleScope, offlineCacheServiceConnection)
        checkDataLoaded()
    }

    override fun onStop() {
        Log.i(TAG, "onStop()")
        // this doesn't stop the service, because we called "startService" explicitly
        ApkOfflineCacheBackgroundService.unbindService(this, offlineCacheServiceConnection)
        super.onStop()
    }

    override fun onLocationPermissionsGranted() {
        tryRegisteringLocationObservers()
    }

    override fun onLocationServiceBind() {
        initializeGpsLocationUpdates()
    }

    private fun initializeGpsLocationUpdates() {
        Log.d(TAG, "initializeGpsLocationUpdates()")

        if (hasLocationPermissions()) {
            val provider =
                if (BuildConfig.USE_TRAIL_PATH_EMULATOR) SdkLocationProvider.EMULATED
                else getPreferredLocationProvider()

            getLocationService()?.requestLocationUpdates(provider)
        } else {
            requestPermissions()
        }
    }

    override fun getViewForSnackbar(): View {
        return binding.root
    }

    override fun getLastLocationLiveData(): MutableLiveData<Location?> {
        return viewModel.lastLocation
    }

    override fun onLocationUpdated(location: Location) {
        Log.i(TAG, "ILocationUpdateListener.onLocationUpdated()")
        Log.d(TAG, location.toString())

        viewModel.setLastLocation(location)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrailInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setSupportActionBar(activity_trail_info_toolbar)

        viewModel = ViewModelProvider(this).get(TrailInfoViewModel::class.java)
        viewModel.loadTechRatings(this)
        mediaService = ServiceFactory.getTrailMediaService(this)

        parseIntent()

        setupToolbar()
        setupTabs()
        setupButtons()
        setupObservers()

        loadTrail()
    }

    override fun onMediaClicked(media: TrailMedia) {
        val intent = MediaDetailActivity.createNavigateToIntent(
            context = this@TrailInfoActivity,
            url = media.url
        )
        startActivity(intent)
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


    protected open fun getPreferredLocationProvider(): SdkLocationProvider? {
        // Read from shared preferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // We need to use default value also here since
        val providerString =  sharedPreferences.getString(UserSettingsActivity.KEY_LOCATION_PROVIDER, null)
        return if (providerString.isNullOrBlank()) {
            null
        } else {
            SdkLocationProvider.valueOf(providerString)
        }
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */
    private fun tryRegisteringLocationObservers() {
        if (hasLocationPermissions()) {
            registerLocationObservers()
        }
    }

    private fun registerLocationObservers() {
        viewModel.lastLocation.observe(this, { lastLocation ->

        })
    }

    private fun setupToolbar() {
        binding.activityTrailInfoBackButton.setOnClickListener {
            onBackPressed()
        }

        /*
        Could not figure out how to show only the back arrow and do not make the toolbar to cast shadow
        So for now replaced with a custom image button.
        TODO Honza: Remove the image button and gix the toolbar to not cast the shadow

        supportActionBar!!.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }*/
    }

    private fun setupTabs() {
        binding.activityTrailDiscoveryTabsContentViewPager.adapter =
            TrailInfoPagerAdapter(supportFragmentManager, this, viewModel.trailId, discussionListener)
    }

    private fun showImages() {
        imageAdapter = TrailMediaAdapter(
            viewModel.imageUrls.value!!,
            mediaService,
            lifecycleScope,
            this,
            this
        )
        binding.activityTrailInfoImageCarousel.adapter = imageAdapter
    }

    private fun setupButtons() {
        binding.activityTrailInfoGetStartIcon.setOnClickListener {
            if (!hasLocationPermissions()) {
                requestPermissions()
            } else {
                if (BuildConfig.USE_TRAIL_PATH_EMULATOR) {
                    // Proximity to trailhead is not required for emulated trail paths.
                    startActivity(DrivingActivity.createNavigateToIntent(this, viewModel.trailId))
                } else if (viewModel.drive.value?.let { trailDrive ->
                        viewModel.getLastLocation()
                            ?.let { location ->
                                TrailNavigator.Direction.isCloseToTrailHeadSegment(
                                    trailDrive,
                                    location
                                )
                            }
                    } == true) {
                    startActivity(DrivingActivity.createNavigateToIntent(this, viewModel.trailId))
                } else {
                    DialogUtil.buildOkDialog(
                        context = this@TrailInfoActivity,
                        title = getString(R.string.start_trail_title),
                        message = getString(R.string.start_trail_message),
                        null
                    ).show()
                }
            }
        }

        binding.activityTrailInfoGetDownloadButton.setOnClickListener {
            lifecycleScope.launchWhenCreated {

                val trailId = viewModel.trailId

                // Let's always include imagery
                val includeImagery = true

                offlineCacheBgService?.offlineMapManager?.let { offlineMapManager ->
                    val trailOfflineMap = offlineMapManager.getTrailOfflineMap(trailId)
                    when (trailOfflineMap?.status ?: OfflineMapStatus.NOT_CACHED) {
                        OfflineMapStatus.NOT_CACHED, OfflineMapStatus.FAILED, OfflineMapStatus.PAUSED,
                        OfflineMapStatus.CANCELED -> {
                            // Please note the estimate is often very inaccurate!
                            // Let's estimate everything
                            val estimateMedia = true
                            val estimateBytes = offlineMapManager.estimateTrailCacheSize(trailId, includeImagery, estimateMedia).totalSize
                            val estimateText = android.text.format.Formatter.formatShortFileSize(
                                this@TrailInfoActivity, estimateBytes)

                            // For the simplification require always 200 MB of free space
                            val mb200 = 209_715_200L
                            val freeBytes = offlineCacheBgService?.offlineMapManager?.getFreeDiskSpace() ?: 0
                            if (freeBytes < mb200) {
                                DialogUtil.buildOkDialog(
                                    context = this@TrailInfoActivity,
                                    title = getString(R.string.download_trail_low_space_title),
                                    message = getString(R.string.download_trail_low_space_message)
                                ).show()
                            } else {
                                // Display DIALOG
                                DialogUtil.buildYesNoCancelDialog(
                                    context = this@TrailInfoActivity,
                                    title = getString(R.string.download),
                                    message = getString(R.string.download_trail_prompt, estimateText),
                                    positiveCodeLabel = "Yes, with media",
                                    positiveCode = {
                                        lifecycleScope.launchWhenCreated {
                                            configureDownloadButton(OfflineMapStatus.WAITING)
                                            val downloadTrailMedia = true
                                            offlineMapManager.downloadTrailOfflineMap(trailId, includeImagery, downloadTrailMedia)
                                        }
                                    },
                                    negativeCodeLabel = "Yes, without media",
                                    negativeCode = {
                                        lifecycleScope.launchWhenCreated {
                                            configureDownloadButton(OfflineMapStatus.WAITING)
                                            val downloadTrailMedia = false
                                            offlineMapManager.downloadTrailOfflineMap(viewModel.trailId, includeImagery, downloadTrailMedia)
                                        }
                                    },
                                    neutralCodeLabel = "No",
                                ).show()
                            }
                        }
                        OfflineMapStatus.COMPLETE ->
                        {
                            requireNotNull(trailOfflineMap) { "OfflineMap not found" }
                            val mapSizeEstimate =
                                offlineMapManager.getOfflineMapStorageSize(trailOfflineMap.offlineMapId)
                            val cacheSize =
                                android.text.format.Formatter.formatShortFileSize(this@TrailInfoActivity,
                                    mapSizeEstimate?.totalSize ?: 0
                                )

                            DialogUtil.buildYesNoDialog(this@TrailInfoActivity,
                                getString(R.string.offline_cache), getString(R.string.trail_already_cached, cacheSize),
                                R.string.remove, R.string.cancel
                            ) {
                                lifecycleScope.launchWhenCreated {
                                    deleteCachedMap(offlineMapManager)
                                }
                            }.show()
                        }
                        OfflineMapStatus.IN_PROGRESS, OfflineMapStatus.WAITING ->
                        {
                            DialogUtil.buildYesNoDialog(this@TrailInfoActivity,
                                getString(R.string.offline_cache), getString(R.string.downloading_trail_cache), R.string.remove, R.string.cancel
                            ) {
                                lifecycleScope.launchWhenCreated {
                                    deleteCachedMap(offlineMapManager)
                                }
                            }.show()
                        }
                    }
                }
            }
        }

        binding.activityTrailInfoFavoriteWrapper.setOnClickListener {
            onFavoriteClicked()
        }

        binding.activityTrailInfoGetThereIcon.setOnClickListener {
            toast("TODO: Navigate to trail")
        }
    }

    private suspend fun deleteCachedMap(offlineMapManager: IOfflineMapManager) {
        // We want to run the DELETE even then the screen is exited
        GlobalScope.launch {
            // Disable the UI
            lifecycleScope.launchWhenResumed {
                binding.activityTrailDiscoveryProgressDialog.visibility = View.VISIBLE
            }
            val offlineMap = offlineMapManager.getTrailOfflineMap(viewModel.trailId) ?: return@launch

            offlineMapManager.deleteOfflineMap(offlineMap.offlineMapId)
            // Enable the UI back and refresh the button
            lifecycleScope.launchWhenResumed {
                configureDownloadButton(OfflineMapStatus.NOT_CACHED)
                binding.activityTrailDiscoveryProgressDialog.visibility = View.GONE
            }
        }
    }

    private fun parseIntent() {
        require(intent.hasExtra(KEY_TRAIL_ID)) { "No intent key $KEY_TRAIL_ID provided." }

        viewModel.trailId = intent.getLongExtra(KEY_TRAIL_ID, Long.MIN_VALUE)
        require(viewModel.trailId != Long.MIN_VALUE) { "Invalid $KEY_TRAIL_ID value." }
    }

    private fun loadTrail() {
        lifecycleScope.launchWhenCreated {
            try {
                viewModel.loadTrail(viewModel.trailId, this@TrailInfoActivity)
            } catch (e: Exception) {
                longToast("Error while loading the trail: ${e.localizedMessage}")
                finish()
            }
        }
    }

    private fun setupObservers() {
        viewModel.trail.observe(this@TrailInfoActivity, { trail ->
            lifecycleScope.launchWhenResumed {
                if (trail != null) {
                    showTrail(trail)
                }
            }
            checkDataLoaded()
        })
        viewModel.imageUrls.observe(this@TrailInfoActivity, {
            showImages()
            checkDataLoaded()
        })
        viewModel.userData.observe(this@TrailInfoActivity, { userData ->
            // Favorite
            val favoriteIconResource = getFavoriteIconResource(userData?.userFavorite ?: false)
            binding.activityTrailInfoFavoriteIcon.setImageResource(favoriteIconResource)
            checkDataLoaded()
        })
    }

    private fun checkDataLoaded() {
        val isLoaded = (viewModel.trail.value != null) && (viewModel.userData.value != null) && (viewModel.imageUrls.value != null)
        showProgressBar(!isLoaded)
    }

    @UiThread
    private suspend fun showTrail(trail: Trail) {
        withContext(Dispatchers.Main) {
            binding.activityTrailInfoTitle.text = trail.info.name
            binding.activityTrailInfoLength.text = Formatter.getDistanceFormatter()
                .formatDistance(this@TrailInfoActivity, trail.statistics.length)

            if (trail.userRating == null) {
                binding.activityTrailInfoRatingValue.visibility = View.GONE
                binding.activityTrailInfoRatingStars.visibility = View.GONE
                binding.activityTrailInfoRatingsCount.visibility = View.GONE
            } else {
                val userRating = trail.userRating!!
                val rating = round(userRating.rating * 10) / 10.0

                binding.activityTrailInfoRatingValue.text = rating.toString()
                binding.activityTrailInfoRatingStars.rating = rating.toFloat()
                binding.activityTrailInfoRatingsCount.text =
                    getString(R.string.rating_count_number, userRating.ratingCount)

                binding.activityTrailInfoNoRatings.visibility = View.GONE
            }

            val techCode = trail.technicalRating.high.code
            val techRating = EnumUtil.getTechRatingForCode(techCode, this@TrailInfoActivity)
            binding.activityTrailInfoDifficulty.text = techRating!!.name
        }
    }

    private fun configureDownloadButton(mapStatus: OfflineMapStatus, progressPercents: Int = 0) {
        when (mapStatus) {
            OfflineMapStatus.NOT_CACHED, OfflineMapStatus.FAILED ->
            {
                binding.activityTrailInfoGetDownloadButton.visibility = View.VISIBLE
                binding.activityTrailInfoGetDownloadIcon.setImageDrawable(loadDrawable(R.drawable.ic_cloud_download_24px))
                binding.activityTrailInfoGetDownloadText.text = getString(R.string.download)
            }
            OfflineMapStatus.WAITING ->
            {
                binding.activityTrailInfoGetDownloadButton.visibility = View.VISIBLE
                binding.activityTrailInfoGetDownloadIcon.setImageDrawable(loadDrawable(R.drawable.ic_cloud_download_24px))
                binding.activityTrailInfoGetDownloadText.text = getString(R.string.queued)
            }
            OfflineMapStatus.IN_PROGRESS ->
            {
                binding.activityTrailInfoGetDownloadButton.visibility = View.VISIBLE
                binding.activityTrailInfoGetDownloadIcon.setImageDrawable(loadDrawable(R.drawable.ic_cloud_download_24px))
                binding.activityTrailInfoGetDownloadText.text = getString(R.string.download_percents, progressPercents)
            }
            OfflineMapStatus.COMPLETE ->
            {
                binding.activityTrailInfoGetDownloadButton.visibility = View.VISIBLE
                binding.activityTrailInfoGetDownloadIcon.setImageDrawable(loadDrawable(R.drawable.ic_cacheinfo_24px))
                binding.activityTrailInfoGetDownloadText.text = getString(R.string.cached)
            }
            OfflineMapStatus.PAUSED ->
            {
                binding.activityTrailInfoGetDownloadButton.visibility = View.VISIBLE
                binding.activityTrailInfoGetDownloadIcon.setImageDrawable(loadDrawable(R.drawable.ic_cloud_download_24px))
                binding.activityTrailInfoGetDownloadText.text = getString(R.string.download)
            }
            OfflineMapStatus.CANCELED ->
            {
                binding.activityTrailInfoGetDownloadButton.visibility = View.VISIBLE
                binding.activityTrailInfoGetDownloadIcon.setImageDrawable(loadDrawable(R.drawable.ic_cloud_download_24px))
                binding.activityTrailInfoGetDownloadText.text = getString(R.string.cancelled)
            }
        }
    }

    /**
     * Post a comment for given [PostTrailCommentRequest]
     */
    private fun postComment(commentRequest: PostTrailCommentRequest) {
        dialogHolder.displayProgressDialog(this, getString(R.string.trails_updating_trail))

        lifecycleScope.launchWhenCreated {
            try {
                val service = ServiceFactory.getTrailService(this@TrailInfoActivity)
                val trailId = viewModel.trailId
                val result = service.postTrailComment(commentRequest)
                if (result.isSuccess) {
                    // Let's reload comments
                    viewModel.loadTrailComments(this@TrailInfoActivity, trailId)
                } else {
                    longToast("Cannot post a comment because of: ${result.errorMessage}")
                    CrashSupport.reportError(result, commentRequest.toString())
                }
            } catch (e: Exception) {
                longToast("Cannot post a comment because of: ${e.localizedMessage}")
                CrashSupport.reportError(e)
            } finally {
                dialogHolder.hideProgressDialog()
            }
        }

    }

    /**
     * Deletes passed [comment]
     */
    private fun deleteComment(comment: TrailComment) {
        dialogHolder.displayProgressDialog(this, getString(R.string.trails_updating_trail))
        val commentUuid = comment.commentUuid

        lifecycleScope.launchWhenCreated {
            try {
                val service = ServiceFactory.getTrailService(this@TrailInfoActivity)
                val result = service.deleteTrailComment(commentUuid)
                if (result.isSuccess) {
                    // Let's reload comments
                    viewModel.loadTrailComments(this@TrailInfoActivity, comment.trailId)
                } else {
                    longToast("Cannot delete a comment $commentUuid because of: ${result.errorMessage}")
                    CrashSupport.reportError(result, comment.toString())
                }
            } catch (e: Exception) {
                longToast("Cannot delete a comment $commentUuid because of: ${e.localizedMessage}")
                CrashSupport.reportError(e, comment.toString())
            } finally {
                dialogHolder.hideProgressDialog()
            }
        }
    }

    private fun onFavoriteClicked() {
        val userData = viewModel.userData.value ?: return
        if (!viewModel.isTrailIdSet()) {
            return
        }

        lifecycleScope.launchWhenCreated {
            // Show progress
            showProgressBar(true)
            // Toggle value
            val toggleFavorite = !(userData.userFavorite ?: false)
            val service = ServiceFactory.getTrailService(this@TrailInfoActivity)
            val result = service.setTrailFavorite(viewModel.trailId, toggleFavorite)
            if (result.isSuccess) {
                // Update the value also in the VM
                viewModel.userData.value = viewModel.userData.value?.copyWithFavorite(result.value?.favorite ?: false)
            } else {
                longToast(getString(R.string.trails_trail_update_failed, result.errorMessage ?: "Unknown error"))
                CrashSupport.reportError(result)
            }
            // Hide progress
            showProgressBar(false)
        }
    }

    private fun onTrailRatingSet(rating: Float) {
        val trailId = viewModel.trailId
        if (trailId < 0) {
            return
        }
        lifecycleScope.launchWhenCreated {
            // Show progress
            showProgressBar(true)
            // Set value
            try {
                val service = ServiceFactory.getTrailService(this@TrailInfoActivity)
                val result = service.setTrailRating(trailId, rating)
                if (result.isSuccess) {
                    // Update the value also in the VM
                    viewModel.userData.value = viewModel.userData.value?.copyWithRating(rating)
                } else {
                    longToast(getString(R.string.trails_trail_update_failed, result.errorMessage ?: "Unknown error"))
                    CrashSupport.reportError(result, "Trail ID: $trailId, rating: $rating")
                }
            } catch (e: Exception) {
                longToast(getString(R.string.trails_trail_update_failed, e.localizedMessage ?: e.toString()))
                CrashSupport.reportError(e, "Trail ID: $trailId, rating: $rating")
            }
            // Hide progress
            showProgressBar(false)
        }
    }


    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    private fun showProgressBar(visible: Boolean) {
        binding.activityTrailInfoProgressBar.visibility = visible.visibility
    }

    private class CacheProgressListener(activity: TrailInfoActivity): ICacheProgressListener {

        val weakActivity = WeakReference(activity)

        override fun onComplete(offlineMap: IOfflineMap) {
            weakActivity.get()?.let { activity ->
                activity.lifecycleScope.launchWhenCreated {
                    if (offlineMap is ITrailOfflineMap && offlineMap.trailId == activity.viewModel.trailId) {
                        activity.configureDownloadButton(OfflineMapStatus.COMPLETE)
                    }
                }
            }
        }

        override fun onComplete() {
            // We do not wan to do anything
        }

        override fun onError(error: HashMap<IOfflineResource, String>, offlineMap: IOfflineMap) {
            weakActivity.get()?.let { activity ->
                activity.lifecycleScope.launchWhenCreated {
                    if (offlineMap is ITrailOfflineMap && offlineMap.trailId == activity.viewModel.trailId) {
                        activity.configureDownloadButton(OfflineMapStatus.FAILED)
                        val errorMessage = error.map { e ->
                            "${e.key.getResourceTypeName()}: ${e.value}"
                        }.joinToString("\n")
                        DialogUtil.buildOkDialog(activity, activity.getString(R.string.download_failed), errorMessage)
                            .show()
                    }
                }
            }
        }

        override fun onProgressChanged(offlineMap: IOfflineMap) {
            weakActivity.get()?.let { activity ->
                activity.lifecycleScope.launchWhenCreated {
                    if (offlineMap is ITrailOfflineMap && offlineMap.trailId == activity.viewModel.trailId) {

                        // Show what is downloading and progress
                        val progressPercents = (100.0 * offlineMap.progress).toInt()

                        activity.configureDownloadButton(OfflineMapStatus.IN_PROGRESS, progressPercents)
                    }
                }
            }
        }

        override fun onImageryDeleted(offlineMaps: List<IOfflineMap>) {
            // We do not want to show anything now
        }
    }

    class UgcListener(activity: TrailInfoActivity): TrailUgcFragment.Listener {

        private val weakActivity = WeakReference(activity)

        override fun onAddTrailComment() {
            val activity = weakActivity.get() ?: return
            val trailId = activity.viewModel.trailId
            if (trailId < 0) {
                return
            }
            // Build and display `Add Comment` dialog
            DialogUtil.buildInputDialog(activity,
                title = activity.getString(R.string.general_comment_add),
                positiveCode = { text ->
                    val comment = PostTrailCommentRequestBuilder.build(trailId, text, null)
                    activity.postComment(comment)
                },
                hint = activity.getString(R.string.general_comment_hint_comment_text),
                positiveCodeLabel = activity.getString(R.string.general_comment_send),
                negativeCodeLabel = activity.getString(R.string.cancel)
            ).show()
        }

        override fun onEditTrailComment(comment: TrailComment) {
            val activity = weakActivity.get() ?: return
            val trailId = activity.viewModel.trailId
            if (trailId < 0) {
                return
            }
            // Build and display `Edit Comment` dialog
            DialogUtil.buildInputDialog(activity,
                title = activity.getString(R.string.general_comment_edit),
                positiveCode = { newText ->
                    val updatedComment = comment.copyWithText(newText)
                    val request = PostTrailCommentRequestBuilder.build(updatedComment)
                    activity.postComment(request)
                },
                hint = activity.getString(R.string.general_comment_hint_comment_text),
                defaultValue = comment.text,
                positiveCodeLabel = activity.getString(R.string.general_comment_update),
                negativeCodeLabel = activity.getString(R.string.cancel)
            ).show()
        }

        override fun onDeleteTrailComment(comment: TrailComment) {
            val activity = weakActivity.get() ?: return
            val trailId = activity.viewModel.trailId
            if (trailId < 0) {
                return
            }
            // Build and display `Delete Comment` dialog
            DialogUtil.buildYesNoDialog(activity,
                title = activity.getString(R.string.general_comment_delete),
                message = activity.getString(R.string.general_comment_delete_dialog_message),
                positiveCode = {
                    activity.deleteComment(comment)
                },
                positiveCodeLabel = activity.getString(R.string.general_delete),
                negativeCodeLabel = activity.getString(R.string.general_cancel)
            ).show()
        }

        override fun onUserRatingSet(rating: Float) {
            weakActivity.get()?.onTrailRatingSet(rating)
        }

    }

    class TrailInfoPagerAdapter(fm: FragmentManager,
                                private val context: Context,
                                private val trailId: Long,
                                private val ugcListener: UgcListener)
        : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private val tabNames = arrayOf(
            R.string.description,
            R.string.general_pois,
            R.string.developer,
            R.string.forecast,
            R.string.general_ugc
        )

        override fun getItem(position: Int): Fragment {
            val arguments = Bundle().apply {
                putLong(TrailInfoFragment.KEY_TRAIL_ID, trailId)
            }

            val fragment = when(position) {
                0 -> {
                    TrailDescriptionFragment()
                }
                1 -> {
                    TrailPoisFragment()
                }
                2 -> {
                    TrailDetailsFragment()
                }
                4 -> {
                    TrailUgcFragment(ugcListener)
                }
                else -> {
                    arguments.putString(DummyTrailInfoFragment.KEY_DUMMY_FRAGMENT_TITLE,
                        getPageTitle(position).toString())

                    DummyTrailInfoFragment()
                }
            }

            fragment.arguments = arguments
            return fragment
        }

        override fun getCount(): Int {
            return tabNames.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return context.getString(tabNames[position])
        }
    }

}
