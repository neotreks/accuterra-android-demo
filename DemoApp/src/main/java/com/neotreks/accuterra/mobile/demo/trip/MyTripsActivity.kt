package com.neotreks.accuterra.mobile.demo.trip

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.neotreks.accuterra.mobile.demo.*
import com.neotreks.accuterra.mobile.demo.databinding.ActivityMyTripsBinding
import com.neotreks.accuterra.mobile.demo.feed.*
import com.neotreks.accuterra.mobile.demo.trip.online.OnlineTripActivity
import com.neotreks.accuterra.mobile.demo.trip.online.OnlineTripTabDefinition
import com.neotreks.accuterra.mobile.demo.trip.recorded.NewTripActivity
import com.neotreks.accuterra.mobile.demo.trip.recorded.RecordedTripActivity
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripRecordingActivity
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripSaveActivity
import com.neotreks.accuterra.mobile.demo.trip.trailcollection.TrailCollectionActivity
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailCollectionData
import com.neotreks.accuterra.mobile.demo.trip.trailcollection.TrailSaveActivity
import com.neotreks.accuterra.mobile.demo.ui.ProgressDialogHolder
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.demo.upload.ObjectUploadActivity
import com.neotreks.accuterra.mobile.demo.util.CrashSupport
import com.neotreks.accuterra.mobile.demo.util.NetworkStateReceiver
import com.neotreks.accuterra.mobile.demo.util.jsonToClass
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.trail.model.MapLocation
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingBasicInfo
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingSearchCriteria
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingStatus
import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry
import com.neotreks.accuterra.mobile.sdk.ugc.model.GetMyActivityFeedCriteria
import com.neotreks.accuterra.mobile.sdk.ugc.model.SetTripLikedResult
import com.neotreks.accuterra.mobile.sdk.ugc.model.TripProcessingStatus
import java.lang.ref.WeakReference

class MyTripsActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val viewModel: MyTripsViewModel by viewModels()
    private lateinit var binding: ActivityMyTripsBinding
    private var dialogHolder = ProgressDialogHolder()

    private lateinit var activityFeedAdapter: ActivityFeedRecycleViewAdapter
    private val activityFeedListener by lazy { FeedListener(WeakReference(this)) }

    private lateinit var networkStateReceiver: NetworkStateReceiver
    
    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "MyTripsActivity"
        private const val REQUEST_ONLINE = 10
        private const val REQUEST_RECORDED = 11
        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, MyTripsActivity::class.java)
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyTripsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        networkStateReceiver = NetworkStateReceiver(this)

        setupToolbar()
        selectCurrentTab()
        setupTabListener()
        setupButtons()
        UiUtils.setApkVersionText(binding.activityMyTripsToolbar.generalToolbarSdkVersion)

        setupListView()
        registerViewModelObservers()
        viewModel.reset()
    }

    override fun onResume() {
        super.onResume()
        loadTrips(forceReload = false)
        updateNewTripButton()
        networkStateReceiver.onResume()
    }

    override fun onPause() {
        super.onPause()
        networkStateReceiver.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ONLINE
            && (resultCode == OnlineTripActivity.RESULT_DELETED || resultCode == OnlineTripActivity.RESULT_COMMENTS_COUNT_UPDATED)) {
            loadTrips(true)
        }
        if (requestCode == REQUEST_RECORDED && resultCode == RecordedTripActivity.RESULT_NOT_FOUND) {
            // Trip was most likely deleted, let's refresh the list
            loadTrips(true)
        }
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun registerViewModelObservers() {
        viewModel.listItems.observe(this, { trips ->
            hideProgressBar()
            activityFeedAdapter.setItems(trips)
            binding.activityMyTripsNoTripsLabel.visibility = trips.isEmpty().visibility
        })
    }

    private fun hideProgressBar() {
        binding.activityMyTripsListSwipeRefresh.isRefreshing = false
    }

    private fun displayProgressBar() {
        binding.activityMyTripsListSwipeRefresh.isRefreshing = true
    }

    private fun loadTrips(forceReload: Boolean) {
        displayProgressBar()
        val isOnline = isOnline()
        if (isOnline) {
            // Check if not already loaded
            if (viewModel.listItems.value?.find { it.type == ActivityFeedItemType.ONLINE_TRIP_HEADER } != null
                && !forceReload) {
                hideProgressBar()
                return
            }
            // Online
            val criteria = GetMyActivityFeedCriteria()
            viewModel.loadOnlineTrips(this, criteria)
        } else {
            // Local
            val criteria = TripRecordingSearchCriteria()
            viewModel.loadRecordedTrips(this, criteria)
        }
    }

    private fun isOnline(): Boolean {
        return binding.activityMyTripsSourceSwitch.isChecked
    }

    private fun setupListView() {
        activityFeedAdapter = ActivityFeedRecycleViewAdapter(context = this,
            items = viewModel.listItems.value ?: listOf(), listener = activityFeedListener,
            lifecycleScope = lifecycleScope)
        binding.activityMyTripsList.layoutManager = LinearLayoutManager(this)
        binding.activityMyTripsList.adapter = activityFeedAdapter
        binding.activityMyTripsListSwipeRefresh.setOnRefreshListener {
            loadTrips(forceReload = true)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.activityMyTripsToolbar.accuterraToolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(false)
            setHomeButtonEnabled(false)
        }
    }

    private fun setupButtons() {
        binding.activityMyTripsAddTripButton.setOnClickListener {
            val intent = NewTripActivity.createNavigateToIntent(this@MyTripsActivity)
            startActivity(intent)
        }
        binding.activityMyTripsSourceSwitch.setOnCheckedChangeListener { _, isChecked ->
            onSourceChanged(isChecked)
        }
    }

    private fun onSourceChanged(isOnlineSource: Boolean) {
        // Change the Label
        if (isOnlineSource) {
            if (networkStateReceiver.isConnected()) {
                // Online
                binding.activityMyTripsSourceSwitch.text = getString(R.string.activity_my_trips_source_online)
                binding.activityMyTripsAddTripButton.visibility = false.visibility
            } else {
                // we need to switch back to offline
                binding.activityMyTripsSourceSwitch.isChecked = false
                longToast(getString(R.string.general_error_no_internet_connection))
            }
        } else {
            // Local
            binding.activityMyTripsSourceSwitch.text = getString(R.string.activity_my_trips_source_local)
            binding.activityMyTripsAddTripButton.visibility = true.visibility
        }
        // Refresh data
        loadTrips(forceReload = true)
    }

    private fun selectCurrentTab() {
        binding.activityMyTripsTabs.componentBasicTabs.getTabAt(AppBasicTabs.TAB_MY_TRIPS_INDEX)!!.select()
    }

    private fun setupTabListener() {
        binding.activityMyTripsTabs.componentBasicTabs.addOnTabSelectedListener(
            MainTabListener(object: MainTabListener.MainTabListenerHelper {
                override val context: Activity
                    get() = this@MyTripsActivity
                override fun getCurrentTabPosition(): Int {
                    return AppBasicTabs.TAB_MY_TRIPS_INDEX
                }
                override fun shouldFinish(): Boolean {
                    return true
                }
                override fun getCommunityFeedLocation(): MapLocation? {
                    return null
                }
            })
        )
    }

    private fun updateNewTripButton() {
        lifecycleScope.launchWhenCreated {
            val recorder = ServiceFactory.getTripRecorder(applicationContext)
            val hasActiveTrip = recorder.hasActiveTripRecording()
            // We cannot allow new recording if there is an active recording already
            binding.activityMyTripsAddTripButton.isEnabled = !hasActiveTrip
            val color = if (hasActiveTrip) {
                ColorStateList.valueOf(getColor(R.color.colorDisabled))
            } else {
                ColorStateList.valueOf(getColor(R.color.colorPrimary))
            }
            binding.activityMyTripsAddTripButton.backgroundTintList = color
        }
    }

    /**
     * Update the model and UI for trip `likes`
     */
    private fun onLikesChanged(likesResult: SetTripLikedResult) {
        // Change the "likes" value
        val item = (viewModel.listItems.value?.find {
            it.tripUUID == likesResult.tripUuid && it.type == ActivityFeedItemType.ONLINE_TRIP_UGC_FOOTER }
                as? ActivityFeedTripUgcFooterItem)
            ?: return
        val tripCopy = item.data?.trip?.copy(userLike = likesResult.userLike, likesCount = likesResult.likes)
            ?: return
        item.data.trip = tripCopy
        // Notify about the change
        activityFeedAdapter.notifyItemChanged(item)
    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    class FeedListener(private val activityRef: WeakReference<MyTripsActivity>) : ActivityFeedViewHolder.Listener {
        override fun onItemClick(item: ActivityFeedItem) {
            val activity = activityRef.get() ?: return
            when(item.type) {
                ActivityFeedItemType.LOCAL_RECORDED_TRIP -> {
                    val trip = item.data as TripRecordingBasicInfo
                    when(trip.status) {
                        TripRecordingStatus.RECORDING,
                        TripRecordingStatus.PAUSED -> {
                            activity.lifecycleScope.launchWhenCreated {
                                // We need to load full trip and the `custom data` to distinguish
                                // between free roam and trail collection recordings
                                val trailCollectionData = getTrailCollectionData(activity, trip.uuid)
                                if (trailCollectionData == null) {
                                    // Navigate to Free Roam recording activity
                                    val intent = TripRecordingActivity.createNavigateToIntent(activity)
                                    activity.startActivity(intent)
                                } else {
                                    // Navigate to Trail Collection recording activity
                                    val intent = TrailCollectionActivity.createNavigateToIntent(activity)
                                    activity.startActivityForResult(intent, REQUEST_RECORDED)
                                }
                            }
                        }
                        TripRecordingStatus.FINISHED -> {
                            activity.lifecycleScope.launchWhenCreated {
                                // We need to load full trip and the `custom data` to distinguish
                                // between free roam and trail collection recordings
                                val trailCollectionData = getTrailCollectionData(activity, trip.uuid)
                                if (trailCollectionData == null) {
                                    // Navigate to the SAVE activity in case of finished recording
                                    val intent = TripSaveActivity.createNavigateToIntent(activity, trip.uuid)
                                    activity.startActivity(intent)
                                } else {
                                    // Navigate to the SAVE activity in case of finished recording
                                    val intent = TrailSaveActivity.createNavigateToIntent(activity, trip.uuid)
                                    activity.startActivity(intent)
                                }
                            }
                        }
                        TripRecordingStatus.QUEUED,
                        TripRecordingStatus.UPLOADED -> {
                            val intent = RecordedTripActivity.createNavigateToIntent(activity, trip.uuid)
                            activity.startActivityForResult(intent, REQUEST_RECORDED)
                        }
                    }
                }
                else -> {
                    // Check trip processing status
                    val processingStatus = (item.data as? ActivityFeedEntry)?.trip?.processingStatus
                    if (processingStatus != TripProcessingStatus.PROCESSED) {
                        activity.toast(activity.getString(R.string.activity_my_trips_cannot_open_trip, processingStatus?.getName() ?: "unknown"))
                        return
                    }
                    // Online trip
                    val intent = OnlineTripActivity.createNavigateToIntent(activity, item.tripUUID)
                    activity.startActivityForResult(intent, REQUEST_ONLINE)
                }
            }
        }

        override fun onLongItemClick(item: ActivityFeedItem): Boolean {
            when(item.type) {
                ActivityFeedItemType.LOCAL_RECORDED_TRIP -> {
                    // Navigate to object upload activity
                    val activity = activityRef.get() ?: return false
                    val intent = ObjectUploadActivity.createNavigateToIntent(activity, item.tripUUID)
                    activity.startActivity(intent)
                    return true
                }
                else -> return false
            }
        }

        override fun onLikeClicked(item: ActivityFeedItem) {
            when(item.type) {
                ActivityFeedItemType.ONLINE_TRIP_UGC_FOOTER -> {
                    val activity = activityRef.get() ?: return
                    val service = ServiceFactory.getTripService(activity)
                    activity.lifecycleScope.launchWhenCreated {
                        activity.dialogHolder.displayProgressDialog(activity, activity.getString(R.string.trips_updating_trip))
                        try {
                            val liked = !((item as ActivityFeedTripUgcFooterItem).data?.trip?.userLike ?: false)
                            val response = service.setTripLiked(item.tripUUID, liked)
                            if (response.isSuccess) {
                                response.value?.let { likes ->
                                    activity.onLikesChanged(likes)
                                }
                            } else {
                                val text = activity.getString(
                                    R.string.trips_trip_update_failed,
                                    response.errorMessage ?: "Unknown error"
                                )
                                activity.longToast(text)
                                CrashSupport.reportError(response)
                            }
                        } catch (e: Exception) {
                            val message = "Error while liking the trip: ${item.tripUUID}"
                            Log.e(TAG, message, e)
                            CrashSupport.reportError(e, message)
                        } finally {
                            activity.dialogHolder.hideProgressDialog()
                        }
                    }
                }
            }
        }

        override fun onPhotosClicked(item: ActivityFeedItem) {
            val activity = activityRef.get() ?: return
            // Check trip processing status
            val processingStatus = (item.data as? ActivityFeedEntry)?.trip?.processingStatus
            if (processingStatus != TripProcessingStatus.PROCESSED) {
                activity.toast(activity.getString(R.string.activity_my_trips_cannot_open_trip, processingStatus?.getName() ?: "unknown"))
                return
            }
            // Open trip detail on photos tab
            val intent = OnlineTripActivity.createNavigateToIntent(activity, item.tripUUID, OnlineTripTabDefinition.PHOTO)
            activity.startActivity(intent)
        }

        /* * * * * * * * * * * * */
        /*        PRIVATE        */
        /* * * * * * * * * * * * */

        private suspend fun getTrailCollectionData(
            context: Context,
            uuid: String
        ): TrailCollectionData? {
            val service = ServiceFactory.getTripRecordingService(context)
            val tripRecording = service.getTripRecordingByUUID(uuid)
            return tripRecording?.extProperties?.firstOrNull()?.data?.jsonToClass(TrailCollectionData::class.java)
        }

    }

}
