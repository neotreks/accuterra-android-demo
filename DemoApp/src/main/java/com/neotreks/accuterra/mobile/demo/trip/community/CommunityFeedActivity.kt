package com.neotreks.accuterra.mobile.demo.trip.community

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.neotreks.accuterra.mobile.demo.*
import com.neotreks.accuterra.mobile.demo.databinding.ActivityCommunityFeedBinding
import com.neotreks.accuterra.mobile.demo.feed.*
import com.neotreks.accuterra.mobile.demo.trip.community.CommunityFeedViewModel.Companion.DEFAULT_LOCATION
import com.neotreks.accuterra.mobile.demo.trip.online.OnlineTripActivity
import com.neotreks.accuterra.mobile.demo.trip.online.OnlineTripTabDefinition
import com.neotreks.accuterra.mobile.demo.trip.recorded.RecordedTripActivity
import com.neotreks.accuterra.mobile.demo.ui.ProgressDialogHolder
import com.neotreks.accuterra.mobile.demo.ui.UiUtils
import com.neotreks.accuterra.mobile.demo.util.CrashSupport
import com.neotreks.accuterra.mobile.demo.util.NetworkStateReceiver
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.location.SdkLocationUtil
import com.neotreks.accuterra.mobile.sdk.trail.model.MapLocation
import com.neotreks.accuterra.mobile.sdk.ugc.model.ActivityFeedEntry
import com.neotreks.accuterra.mobile.sdk.ugc.model.GetCommunityFeedCriteria
import com.neotreks.accuterra.mobile.sdk.ugc.model.SetTripLikedResult
import com.neotreks.accuterra.mobile.sdk.ugc.model.TripProcessingStatus
import java.lang.ref.WeakReference

class CommunityFeedActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val viewModel: CommunityFeedViewModel by viewModels()
    private var dialogHolder = ProgressDialogHolder()

    private lateinit var communityFeedAdapter: ActivityFeedRecycleViewAdapter
    private val activityFeedListener by lazy { FeedListener(WeakReference(this)) }

    private lateinit var networkStateReceiver: NetworkStateReceiver

    private lateinit var binding: ActivityCommunityFeedBinding

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val TAG = "CommunityFeedActivity"

        private const val KEY_LAT = "lan"
        private const val KEY_LON = "lon"

        private const val REQUEST_ONLINE = 14
        private const val REQUEST_RECORDED = 15

        fun createNavigateToIntent(context: Context, location: MapLocation?): Intent {
            val intent = Intent(context, CommunityFeedActivity::class.java)
            if (location != null) {
                intent.putExtra(KEY_LAT, location.latitude)
                intent.putExtra(KEY_LON, location.longitude)
            }
            return intent
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        networkStateReceiver = NetworkStateReceiver(this)

        setupToolbar()
        selectCurrentTab()
        setupTabListener()
        UiUtils.setApkVersionText(binding.activityCommunityFeedToolbar.generalToolbarSdkVersion)

        setupListView()
        registerViewModelObservers()
        viewModel.reset()
        parseIntent()
    }

    override fun onResume() {
        super.onResume()
        loadTrips(forceReload = false)
        networkStateReceiver.onResume()
    }

    override fun onPause() {
        super.onPause()
        networkStateReceiver.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ONLINE && resultCode == OnlineTripActivity.RESULT_DELETED) {
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

    private fun parseIntent() {

        val lat = intent.getDoubleExtra(KEY_LAT, Double.MIN_VALUE)
        val lon = intent.getDoubleExtra(KEY_LON, Double.MIN_VALUE)

        if (lat != Double.MIN_VALUE && lon != Double.MIN_VALUE) {
            // Let's take the location from the intent extras
            viewModel.location = MapLocation(lat, lon)
        }

    }

    private fun registerViewModelObservers() {
        viewModel.listItems.observe(this, { trips ->
            hideProgressBar()
            communityFeedAdapter.setItems(trips)
            binding.activityCommunityFeedNoTripsLabel.visibility = trips.isEmpty().visibility
        })
    }

    private fun hideProgressBar() {
        binding.activityCommunityFeedListSwipeRefresh.isRefreshing = false
    }

    private fun displayProgressBar() {
        binding.activityCommunityFeedListSwipeRefresh.isRefreshing = true
    }

    private fun loadTrips(forceReload: Boolean) {
        displayProgressBar()
        lifecycleScope.launchWhenCreated {
            if (!viewModel.hasLocation()) {
                // Try to get the location
                viewModel.location = SdkLocationUtil.getLastKnownMapLocation(this@CommunityFeedActivity, 2_000, DEFAULT_LOCATION)
            }
            val isOnline = isOnline()
            if (isOnline) {
                // Check if not already loaded
                if (viewModel.listItems.value?.find { it.type == ActivityFeedItemType.ONLINE_TRIP_HEADER } != null
                    && !forceReload) {
                    hideProgressBar()
                    return@launchWhenCreated
                }
                // Online
                val location = viewModel.location
                val criteria = GetCommunityFeedCriteria(location) // Default criteria
                viewModel.loadCommunityTrips(this@CommunityFeedActivity, criteria)
            } else {
                hideProgressBar()
                longToast(getString(R.string.general_error_no_internet_connection))
            }
        }
    }

    private fun isOnline(): Boolean {
        return networkStateReceiver.isConnected()
    }

    private fun setupListView() {
        communityFeedAdapter = ActivityFeedRecycleViewAdapter(context = this,
            items = viewModel.listItems.value ?: listOf(), listener = activityFeedListener,
            lifecycleScope = lifecycleScope)
        binding.activityCommunityFeedList.layoutManager = LinearLayoutManager(this)
        binding.activityCommunityFeedList.adapter = communityFeedAdapter
        binding.activityCommunityFeedListSwipeRefresh.setOnRefreshListener {
            loadTrips(forceReload = true)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.activityCommunityFeedToolbar.accuterraToolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(false)
            setHomeButtonEnabled(false)
        }
    }

    private fun selectCurrentTab() {
        binding.activityCommunityFeedTabs.componentBasicTabs.getTabAt(AppBasicTabs.TAB_COMMUNITY_INDEX)!!.select()
    }

    private fun setupTabListener() {
        binding.activityCommunityFeedTabs.componentBasicTabs.addOnTabSelectedListener(
            MainTabListener(object: MainTabListener.MainTabListenerHelper {
                override val context: Activity
                    get() = this@CommunityFeedActivity
                override fun getCurrentTabPosition(): Int {
                    return AppBasicTabs.TAB_COMMUNITY_INDEX
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
        communityFeedAdapter.notifyItemChanged(item)
    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    class FeedListener(private val activityRef: WeakReference<CommunityFeedActivity>) : ActivityFeedViewHolder.Listener {

        override fun onItemClick(item: ActivityFeedItem) {
            val activity = activityRef.get() ?: return
            if (item.type != ActivityFeedItemType.LOCAL_RECORDED_TRIP) {
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

        override fun onLongItemClick(item: ActivityFeedItem): Boolean {
            return false // Nothing for long clicks
        }

        override fun onLikeClicked(item: ActivityFeedItem) {
            if (item.type == ActivityFeedItemType.ONLINE_TRIP_UGC_FOOTER) {
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
                            CrashSupport.reportError(response, "onLikeClicked(), item = $item")
                            activity.longToast(text)
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
