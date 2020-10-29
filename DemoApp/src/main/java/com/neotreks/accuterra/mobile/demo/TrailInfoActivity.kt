package com.neotreks.accuterra.mobile.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.extensions.getFavoriteIconResource
import com.neotreks.accuterra.mobile.demo.settings.Formatter
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.demo.ui.ProgressDialogHolder
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.demo.util.EnumUtil
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.map.cache.*
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailMedia
import com.neotreks.accuterra.mobile.sdk.trail.model.Trail
import com.neotreks.accuterra.mobile.sdk.trail.service.ITrailMediaService
import com.neotreks.accuterra.mobile.sdk.ugc.model.PostTrailCommentRequest
import com.neotreks.accuterra.mobile.sdk.ugc.model.PostTrailCommentRequestBuilder
import com.neotreks.accuterra.mobile.sdk.ugc.model.TrailComment
import kotlinx.android.synthetic.main.activity_trail_info.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import kotlin.math.round


class TrailInfoActivity : AppCompatActivity(), TrailMediaClickListener {

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

    private var dialogHolder = ProgressDialogHolder()

    private lateinit var imageAdapter: TrailMediaAdapter

    private lateinit var mediaService: ITrailMediaService

    private var offlineMapService: OfflineMapService? = null

    private val cacheProgressListener = CacheProgressListener(this)

    private val discussionListener = UgcListener(this)

    private val connectionListener = object: OfflineMapServiceConnectionListener {
        override fun onConnected(service: OfflineMapService) {
            offlineMapService = service
            lifecycleScope.launchWhenCreated {
                configureDownloadButton(service.offlineMapManager.getOfflineMapStatus(OfflineMapType.TRAIL, viewModel.trailId))
            }
        }

        override fun onDisconnected() {
            offlineMapService = null
            Log.e(TAG, "offlineMapService disconnected")
        }
    }

    private val offlineMapServiceConnection = OfflineMapService.createServiceConnection(
        connectionListener,
        cacheProgressListener
    )

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onStart() {
        super.onStart()
        Intent(this, OfflineMapService::class.java).also { intent ->
            bindService(intent, offlineMapServiceConnection, Context.BIND_AUTO_CREATE)
        }
        checkDataLoaded()
    }

    override fun onStop() {
        super.onStop()
        // this doesn't stop the service, because we called "startService" explicitly
        unbindService(offlineMapServiceConnection)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, OfflineMapService::class.java))
        setContentView(R.layout.activity_trail_info)
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

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun setupToolbar() {
        activity_trail_info_back_button.setOnClickListener {
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
        activity_trail_discovery_tabs_content_view_pager.adapter =
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
        activity_trail_info_image_carousel.adapter = imageAdapter
    }

    private fun setupButtons() {
        activity_trail_info_get_start_icon.setOnClickListener {
            startActivity(DrivingActivity.createNavigateToIntent(this, viewModel.trailId))
        }

        activity_trail_info_get_download_button.setOnClickListener {
            lifecycleScope.launchWhenCreated {

                offlineMapService?.offlineMapManager?.let { offlineMapManager ->
                    when (offlineMapManager.getOfflineMapStatus(OfflineMapType.TRAIL, viewModel.trailId)) {
                        OfflineMapStatus.NOT_CACHED, OfflineMapStatus.FAILED -> {
                            // Please note the estimate is often very inaccurate!
                            val estimateBytes = offlineMapManager.estimateTrailCacheSize(viewModel.trailId)
                            val estimateText = android.text.format.Formatter.formatShortFileSize(
                                this@TrailInfoActivity, estimateBytes)

                            // For the simplification require always 200 MB of free space
                            val mb200 = 209_715_200L
                            val freeBytes = offlineMapService?.offlineMapManager?.getFreeDiskSpace() ?: 0
                            if (freeBytes < mb200) {
                                DialogUtil.buildOkDialog(
                                    context = this@TrailInfoActivity,
                                    title = getString(R.string.download_trail_low_space_title),
                                    message = getString(R.string.download_trail_low_space_message)
                                ).show()
                            } else {
                                // Display DIALOG
                                DialogUtil.buildYesNoDialog(
                                    context = this@TrailInfoActivity,
                                    title = getString(R.string.download),
                                    message = getString(R.string.download_trail_prompt, estimateText),
                                    code = {
                                        lifecycleScope.launchWhenCreated {
                                            configureDownloadButton(OfflineMapStatus.WAITING)
                                            offlineMapManager.downloadOfflineMap(OfflineMapType.TRAIL, viewModel.trailId)
                                        }
                                    }
                                ).show()
                            }
                        }
                        OfflineMapStatus.COMPLETE ->
                        {
                            val cacheSize =
                                android.text.format.Formatter.formatShortFileSize(this@TrailInfoActivity,
                                    offlineMapManager.getOfflineMapStorageSize(OfflineMapType.TRAIL, viewModel.trailId))

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

        activity_trail_info_favorite_wrapper.setOnClickListener {
            onFavoriteClicked()
        }

        activity_trail_info_get_there_icon.setOnClickListener {
            toast("TODO: Navigate to trail")
        }
    }

    private suspend fun deleteCachedMap(offlineMapManager: IOfflineMapManager) {
        // We want to run the DELETE even then the screen is exited
        GlobalScope.launch {
            // Disable the UI
            lifecycleScope.launchWhenResumed {
                activity_trail_discovery_progress_dialog.visibility = View.VISIBLE
            }
            offlineMapManager.deleteOfflineMap(OfflineMapType.TRAIL, viewModel.trailId)
            // Enable the UI back and refresh the button
            lifecycleScope.launchWhenResumed {
                configureDownloadButton(OfflineMapStatus.NOT_CACHED)
                activity_trail_discovery_progress_dialog.visibility = View.GONE
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
        viewModel.trail.observe(this@TrailInfoActivity, Observer { trail ->
            lifecycleScope.launchWhenResumed {
                if (trail != null) {
                    showTrail(trail)
                }
            }
            checkDataLoaded()
        })
        viewModel.imageUrls.observe(this@TrailInfoActivity, Observer {
            showImages()
            checkDataLoaded()
        })
        viewModel.userData.observe(this@TrailInfoActivity, Observer { userData ->
            // Favorite
            val favoriteIconResource = getFavoriteIconResource(userData?.userFavorite ?: false)
            activity_trail_info_favorite_icon.setImageResource(favoriteIconResource)
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
            activity_trail_info_title.text = trail.info.name
            activity_trail_info_length.text = Formatter.getDistanceFormatter()
                .formatDistance(this@TrailInfoActivity, trail.statistics.length)

            if (trail.userRating == null) {
                activity_trail_info_rating_value.visibility = View.GONE
                activity_trail_info_rating_stars.visibility = View.GONE
                activity_trail_info_ratings_count.visibility = View.GONE
            } else {
                val userRating = trail.userRating!!
                val rating = round(userRating.rating * 10) / 10.0

                activity_trail_info_rating_value.text = rating.toString()
                activity_trail_info_rating_stars.rating = rating.toFloat()
                activity_trail_info_ratings_count.text =
                    getString(R.string.rating_count_number, userRating.ratingCount)

                activity_trail_info_no_ratings.visibility = View.GONE
            }

            val techCode = trail.technicalRating.high.code
            val techRating = EnumUtil.getTechRatingForCode(techCode, this@TrailInfoActivity)
            activity_trail_info_difficulty.text = techRating!!.name
        }
    }

    private fun configureDownloadButton(mapStatus: OfflineMapStatus, progressPercents: Int = 0) {
        when (mapStatus) {
            OfflineMapStatus.NOT_CACHED, OfflineMapStatus.FAILED ->
            {
                activity_trail_info_get_download_button.visibility = View.VISIBLE
                activity_trail_info_get_download_icon.setImageDrawable(loadDrawable(R.drawable.ic_cloud_download_24px))
                activity_trail_info_get_download_text.text = getString(R.string.download)
            }
            OfflineMapStatus.WAITING ->
            {
                activity_trail_info_get_download_button.visibility = View.VISIBLE
                activity_trail_info_get_download_icon.setImageDrawable(loadDrawable(R.drawable.ic_cloud_download_24px))
                activity_trail_info_get_download_text.text = getString(R.string.queued)
            }
            OfflineMapStatus.IN_PROGRESS ->
            {
                activity_trail_info_get_download_button.visibility = View.VISIBLE
                activity_trail_info_get_download_icon.setImageDrawable(loadDrawable(R.drawable.ic_cloud_download_24px))
                activity_trail_info_get_download_text.text = getString(R.string.download_percents, progressPercents)
            }
            OfflineMapStatus.COMPLETE ->
            {
                activity_trail_info_get_download_button.visibility = View.VISIBLE
                activity_trail_info_get_download_icon.setImageDrawable(loadDrawable(R.drawable.ic_cacheinfo_24px))
                activity_trail_info_get_download_text.text = getString(R.string.cached)
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
                }
            } catch (e: Exception) {
                longToast("Cannot post a comment because of: ${e.localizedMessage}")
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
                }
            } catch (e: Exception) {
                longToast("Cannot delete a comment $commentUuid because of: ${e.localizedMessage}")
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
                }
            } catch (e: Exception) {
                longToast(getString(R.string.trails_trail_update_failed, e.localizedMessage ?: e.toString()))
            }
            // Hide progress
            showProgressBar(false)
        }
    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    private fun showProgressBar(visible: Boolean) {
        activity_trail_info_progress_bar.visibility = visible.visibility
    }

    private class CacheProgressListener(activity: TrailInfoActivity):
        com.neotreks.accuterra.mobile.sdk.map.cache.CacheProgressListener {

        val weakActivity = WeakReference(activity)

        override fun onComplete(mapType: OfflineMapType, trailId: Long) {
            weakActivity.get()?.let {
                if (trailId == it.viewModel.trailId) {
                    it.configureDownloadButton(OfflineMapStatus.COMPLETE)
                }
            }
        }

        override fun onError(error: HashMap<IOfflineResource, String>, mapType: OfflineMapType, trailId: Long) {
            weakActivity.get()?.let {
                if (trailId == it.viewModel.trailId) {
                    it.configureDownloadButton(OfflineMapStatus.FAILED)
                    val errorMessage = error.map { e ->
                        "${e.key.getResourceTypeName()}: ${e.value}"
                    }.joinToString("\n")
                    DialogUtil.buildOkDialog(it, it.getString(R.string.download_failed), errorMessage)
                        .show()
                }
            }
        }

        override fun onProgressChanged(progress: Double, mapType: OfflineMapType, trailId: Long) {
            weakActivity.get()?.let {
                if (trailId == it.viewModel.trailId) {


                    // Show what is downloading and progress
                    val progressPercents = (100.0 * progress).toInt()

                    it.configureDownloadButton(OfflineMapStatus.IN_PROGRESS, progressPercents)
                }
            }
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

        override fun getPageTitle(position: Int): CharSequence? {
            return context.getString(tabNames[position])
        }
    }

}
