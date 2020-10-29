package com.neotreks.accuterra.mobile.demo.trip.online

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.neotreks.accuterra.mobile.demo.DemoApplication
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.extensions.isNotNullNorBlank
import com.neotreks.accuterra.mobile.sdk.trip.model.TripMedia
import com.neotreks.accuterra.mobile.sdk.trip.model.TripPoint
import kotlinx.android.synthetic.main.activity_online_trip_poi.*
import kotlinx.android.synthetic.main.general_toolbar.*

class OnlineTripPoiActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: OnlineTripViewModel

    private lateinit var mediaListManager: OnlineTripMediaListManager

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        private const val TAG = "OnlineTripPoiActivity"

        private const val KEY_TRIP_UUID = "TRIP_UUID"
        private const val KEY_POI_UUID = "POI_UUID"

        fun createNavigateToIntent(context: Context, tripUuid: String, poiUuid: String): Intent {
            return Intent(context, OnlineTripPoiActivity::class.java)
                .apply {
                    putExtra(KEY_TRIP_UUID, tripUuid)
                    putExtra(KEY_POI_UUID, poiUuid)
                }
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online_trip_poi)

        setupToolbar()
        setupMediaList()

        initViewModel()
        setupObservers()

        parseIntent()

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

    private fun initViewModel() {
        // Please note it is taken from ViewModelStore because it is shared between more activities
        val viewModelStore = (application as DemoApplication).onlineTripVMStore
        viewModel = ViewModelProvider(viewModelStore, defaultViewModelProviderFactory).get(OnlineTripViewModel::class.java)
    }

    private fun setupObservers() {
        viewModel.trip.observe(this, Observer { trip ->
            if (trip == null) {
                return@Observer
            }
            val poi = trip.navigation.points.find { it.uuid == viewModel.selectedPoiUuid }
                ?: return@Observer
            loadPoi(poi)
        })
    }

    private fun loadPoi(poi: TripPoint) {
        activity_online_trip_poi_trip_name.text = poi.name
        activity_online_trip_poi_trip_description.text = poi.description
        activity_online_trip_poi_is_wp.isChecked = poi.isWaypoint
        activity_online_trip_poi_poi_type.text = poi.pointSubtype.name
        // Load photos
        viewModel.selectedPoiUuid = poi.uuid
        mediaListManager.refreshPhotoGridAdapter(poi.media)
    }

    private fun setupMediaList() {
        activity_online_trip_poi_photos.layoutManager = GridLayoutManager(this, 4)
        mediaListManager = OnlineTripMediaListManager(this, lifecycleScope,
            activity_online_trip_poi_photos,
            object: OnlineTripMediaListManager.TripMediaListClickListener {
                override fun onItemClicked(media: TripMedia) {
                    viewModel.selectedMediaUrl = media.url
                    val intent = OnlineTripMediaActivity.createNavigateToIntent(
                        this@OnlineTripPoiActivity,
                        tripUuid = viewModel.tripUuid,
                        selectedPoiUuid = viewModel.selectedPoiUuid,
                        selectedUrl = media.url
                    )
                    startActivity(intent)
                }
                override fun onDeleteItemClicked(media: TripMedia) {}
            }, readOnly = true)
    }

    private fun parseIntent() {
        // Parse + set the trail ID
        require(intent.hasExtra(KEY_TRIP_UUID)) { "No intent key $KEY_TRIP_UUID provided." }

        val tripUuid = intent.getStringExtra(KEY_TRIP_UUID)
        check(tripUuid.isNotNullNorBlank()) { "Invalid $KEY_TRIP_UUID value." }

        val poiUuid = intent.getStringExtra(KEY_POI_UUID)
        check(poiUuid.isNotNullNorBlank()) { "Invalid $KEY_POI_UUID value." }
        viewModel.selectedPoiUuid = poiUuid

        Log.i(TAG, "id=$tripUuid")
        lifecycleScope.launchWhenCreated {
            if (viewModel.trip.value == null || viewModel.tripUuid != tripUuid) {
                viewModel.tripUuid = tripUuid!!
                viewModel.loadTrip(this@OnlineTripPoiActivity, tripUuid)
            }
        }
    }

    private fun setupToolbar() {

        setSupportActionBar(general_toolbar)
        general_toolbar_title.text = getString(R.string.general_poi)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

    }

}