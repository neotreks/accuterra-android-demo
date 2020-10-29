package com.neotreks.accuterra.mobile.demo.trip

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.neotreks.accuterra.mobile.demo.*
import com.neotreks.accuterra.mobile.demo.feed.*
import com.neotreks.accuterra.mobile.demo.trip.online.OnlineTripActivity
import com.neotreks.accuterra.mobile.demo.trip.online.OnlineTripTabDefinition
import com.neotreks.accuterra.mobile.demo.trip.recorded.NewTripActivity
import com.neotreks.accuterra.mobile.demo.trip.recorded.RecordedTripActivity
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripRecordingActivity
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripSaveActivity
import com.neotreks.accuterra.mobile.demo.ui.ProgressDialogHolder
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.demo.upload.ObjectUploadActivity
import com.neotreks.accuterra.mobile.demo.util.NetworkStateReceiver
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
import kotlinx.android.synthetic.main.accuterra_toolbar.*
import kotlinx.android.synthetic.main.activity_my_trips.*
import kotlinx.android.synthetic.main.component_basic_tabs.*
import java.lang.ref.WeakReference

class MyTripsActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val viewModel: MyTripsViewModel by viewModels()
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
        setContentView(R.layout.activity_my_trips)

        networkStateReceiver = NetworkStateReceiver(this)

        setupToolbar()
        selectCurrentTab()
        setupTabListener()
        setupButtons()
        UiUtils.setApkVersionText(general_toolbar_sdk_version)

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
            activity_my_trips_no_trips_label.visibility = trips.isEmpty().visibility
        })
    }

    private fun hideProgressBar() {
        activity_my_trips_list_swipe_refresh.isRefreshing = false
    }

    private fun displayProgressBar() {
        activity_my_trips_list_swipe_refresh.isRefreshing = true
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
        return activity_my_trips_source_switch.isChecked
    }

    private fun setupListView() {
        activityFeedAdapter = ActivityFeedRecycleViewAdapter(context = this,
            items = viewModel.listItems.value ?: listOf(), listener = activityFeedListener,
            lifecycleScope = lifecycleScope)
        activity_my_trips_list.layoutManager = LinearLayoutManager(this)
        activity_my_trips_list.adapter = activityFeedAdapter
        activity_my_trips_list_swipe_refresh.setOnRefreshListener {
            loadTrips(forceReload = true)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(accuterra_toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(false)
            setHomeButtonEnabled(false)
        }
    }

    private fun setupButtons() {
        activity_my_trips_add_trip_button.setOnClickListener {
            val intent = NewTripActivity.createNavigateToIntent(this@MyTripsActivity)
            startActivity(intent)
        }
        activity_my_trips_source_switch.setOnCheckedChangeListener { _, isChecked ->
            onSourceChanged(isChecked)
        }
    }

    private fun onSourceChanged(isOnlineSource: Boolean) {
        // Change the Label
        if (isOnlineSource) {
            if (networkStateReceiver.isConnected()) {
                // Online
                activity_my_trips_source_switch.text = getString(R.string.activity_my_trips_source_online)
                activity_my_trips_add_trip_button.visibility = false.visibility
            } else {
                // we need to switch back to offline
                activity_my_trips_source_switch.isChecked = false
                longToast(getString(R.string.general_error_no_internet_connection))
            }
        } else {
            // Local
            activity_my_trips_source_switch.text = getString(R.string.activity_my_trips_source_local)
            activity_my_trips_add_trip_button.visibility = true.visibility
        }
        // Refresh data
        loadTrips(forceReload = true)
    }

    private fun selectCurrentTab() {
        component_basic_tabs.getTabAt(AppBasicTabs.TAB_MY_TRIPS_INDEX)!!.select()
    }

    private fun setupTabListener() {
        component_basic_tabs.addOnTabSelectedListener(
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
            val hasActiveTrip = recorder.hasActiveTrip()
            // We cannot allow new recording if there is an active recording already
            activity_my_trips_add_trip_button.isEnabled = !hasActiveTrip
            val color = if (hasActiveTrip) {
                ColorStateList.valueOf(getColor(R.color.colorDisabled))
            } else {
                ColorStateList.valueOf(getColor(R.color.colorPrimary))
            }
            activity_my_trips_add_trip_button.backgroundTintList = color
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
                            // Navigate to recording activity in case of active recording
                            val intent = TripRecordingActivity.createNavigateToIntent(activity)
                            activity.startActivity(intent)
                        }
                        TripRecordingStatus.FINISHED -> {
                            // Navigate to the SAVE activity in case of finished recording
                            val intent = TripSaveActivity.createNavigateToIntent(activity, trip.uuid)
                            activity.startActivity(intent)
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
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error while liking the trip: ${item.tripUUID}", e)
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
    }

}
