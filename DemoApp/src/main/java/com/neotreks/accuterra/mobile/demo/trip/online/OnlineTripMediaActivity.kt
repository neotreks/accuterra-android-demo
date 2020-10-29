package com.neotreks.accuterra.mobile.demo.trip.online

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.neotreks.accuterra.mobile.demo.DemoApplication
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.extensions.isNotNullNorBlank
import com.neotreks.accuterra.mobile.demo.media.ApkMediaVariant
import com.neotreks.accuterra.mobile.demo.ui.RecycleViewBottomDotDecorator
import com.neotreks.accuterra.mobile.sdk.trip.model.Trip
import com.neotreks.accuterra.mobile.sdk.trip.model.TripMedia
import kotlinx.android.synthetic.main.activity_online_trip_media.*
import kotlinx.android.synthetic.main.general_toolbar.*

class OnlineTripMediaActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: OnlineTripViewModel

    private lateinit var mediaAdapter: OnlineTripMediaFileViewAdapter

    private val bottomDotIndicator = RecycleViewBottomDotDecorator()

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "OnlineTripMediaActivity"

        private const val KEY_TRIP_UUID = "TRIP_UUID"
        private const val KEY_SELECTED_POI_UUID = "SELECTED_POI_UUID"
        private const val KEY_SELECTED_MEDIA_URL = "SELECTED_MEDIA_URL"

        fun createNavigateToIntent(context: Context, tripUuid: String,
                                   selectedPoiUuid: String? = null,
                                   selectedUrl: String? = null): Intent {
            return Intent(context, OnlineTripMediaActivity::class.java)
                .apply {
                    putExtra(KEY_TRIP_UUID, tripUuid)
                    putExtra(KEY_SELECTED_POI_UUID, selectedPoiUuid)
                    putExtra(KEY_SELECTED_MEDIA_URL, selectedUrl)
                }
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online_trip_media)

        initViewModel()
        setupToolbar()
        setupRecycleView()
        setupObservers()

        parseIntent()
    }

    private fun initViewModel() {
        // Please note it is taken from ViewModelStore because it is shared between more activities
        val viewModelStore = (application as DemoApplication).onlineTripVMStore
        viewModel = ViewModelProvider(viewModelStore, defaultViewModelProviderFactory).get(OnlineTripViewModel::class.java)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun setupToolbar() {

        setSupportActionBar(general_toolbar)
        general_toolbar_title.text = getString(R.string.general_photos)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

    }

    private fun setupRecycleView() {
        mediaAdapter = OnlineTripMediaFileViewAdapter(
            this,
            lifecycleScope,
            arrayOf(),
            ApkMediaVariant.THUMBNAIL,
            true,
            R.layout.component_image_view_full_screen
        )
        activity_online_trip_media_recycle_view.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        // Attach the adapter
        activity_online_trip_media_recycle_view.adapter = mediaAdapter
        // add pager behavior
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(activity_online_trip_media_recycle_view)
        // pager indicator
        activity_online_trip_media_recycle_view.addItemDecoration(bottomDotIndicator)
        // Set listener for clicking decorator's `bottom dots`
        activity_online_trip_media_recycle_view.addOnItemTouchListener(bottomDotIndicator.indicatorTouchListener)
    }

    private fun setupObservers() {
        viewModel.trip.observe(this, Observer { trip ->
            if (trip == null) {
                return@Observer
            }
            val media = buildMediaList(trip, viewModel.selectedPoiUuid)
            mediaAdapter = buildMediaAdapter(media)
            bottomDotIndicator.reset()
            activity_online_trip_media_recycle_view.adapter = mediaAdapter
            // Scroll to selected position if exists
            val url = viewModel.selectedMediaUrl
            if (url.isNotNullNorBlank()) {
                scrollToImageByUrl(url!!)
            }
        })
    }

    /**
     * Builds list of media for:
     * * The whole trip (trip media + trip pois media)
     * * or for single POINT
     */
    private fun buildMediaList(trip: Trip, selectedPoiUuid: String?): List<TripMedia> {
        val media = if (selectedPoiUuid.isNullOrBlank()) {
            // The whole trip
            trip.media + trip.navigation.points.flatMap { point ->
                point.media.map { it }
            }
        } else {
            // For single POINT
            trip.navigation.points.find { it.uuid == selectedPoiUuid }?.media?.map { it }
        }
        return media ?: listOf()
    }

    private fun buildMediaAdapter(media: List<TripMedia>): OnlineTripMediaFileViewAdapter {
        return OnlineTripMediaFileViewAdapter(
            this, lifecycleScope, media.toTypedArray(), ApkMediaVariant.DEFAULT,
            true,
            R.layout.component_image_view_full_screen
        )
    }

    private fun parseIntent() {
        // Parse + set the trail ID
        require(intent.hasExtra(KEY_TRIP_UUID)) { "No intent key $KEY_TRIP_UUID provided." }

        val tripUuid = intent.getStringExtra(KEY_TRIP_UUID)
        check(tripUuid.isNotNullNorBlank()) { "Invalid $KEY_TRIP_UUID value." }

        val poiUuid = intent.getStringExtra(KEY_SELECTED_POI_UUID)
        viewModel.selectedPoiUuid = poiUuid

        val url = intent.getStringExtra(KEY_SELECTED_MEDIA_URL)
        viewModel.selectedMediaUrl = url

        Log.d(TAG, "tripUUID=$tripUuid")
        lifecycleScope.launchWhenCreated {
            if (viewModel.trip.value == null || viewModel.tripUuid != tripUuid) {
                viewModel.loadTrip(this@OnlineTripMediaActivity, tripUuid!!)
            }
        }
    }

    private fun scrollToImageByUrl(url: String) {
        val position = mediaAdapter.getItemPositionBy(url)
        if (position == -1) {
            Log.w(TAG, "Item position not found for url: $url")
            return
        }
        activity_online_trip_media_recycle_view.layoutManager?.scrollToPosition(position)
    }

}