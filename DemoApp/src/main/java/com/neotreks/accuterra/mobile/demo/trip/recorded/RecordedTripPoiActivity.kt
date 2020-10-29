package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import kotlinx.android.synthetic.main.activity_recorded_trip_poi.*
import kotlinx.android.synthetic.main.general_action_toolbar.*

class RecordedTripPoiActivity : AppCompatActivity() {

    private val viewModel: RecordedTripPoiViewModel by viewModels()

    private lateinit var mediaListManager: TripRecordingMediaListManager

    companion object {

        private const val TAG = "RecordedTripPoiActivity"

        private const val KEY_POI_UUID = "KEY_POI_UUID"

        fun createNavigateToIntent(poiUuid: String?, context: Context): Intent {
            return Intent(context, RecordedTripPoiActivity::class.java)
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
        setContentView(R.layout.activity_recorded_trip_poi)
        mediaListManager = TripRecordingMediaListManager(this,
            activity_recorded_trip_poi_photos,
            object: TripRecordingMediaListManager.TripRecordingMediaListClickListener {
                override fun onItemClicked(media: TripRecordingMedia) {
                    val intent = MediaDetailActivity.createNavigateToIntent(
                        context = this@RecordedTripPoiActivity,
                        mediaUuid = media.uuid
                    )
                    startActivity(intent)
                }
                override fun onDeleteItemClicked(media: TripRecordingMedia) {
                    throw IllegalStateException("Media deletion not supported.")
                }
            }, readOnly = true
        )

        lifecycleScope.launchWhenCreated {
            setupToolbar()
            mediaListManager.setupPhotoGrid()
            setupObservers()
            // Load data as the last step when all is initialized
            parseIntent()
        }
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

    private suspend fun parseIntent() {

        val poiUuid = intent.getStringExtra(KEY_POI_UUID)
        check(poiUuid != null) { "Invalid $KEY_POI_UUID value." }

        Log.i(TAG, "poiUuid = $poiUuid")
        viewModel.loadPoi(poiUuid, this)
    }

    private fun setupToolbar() {

        setSupportActionBar(action_toolbar)
        action_toolbar_title.text = getString(R.string.activity_recorded_trip_poi_title)
        action_toolbar_action.text = ""

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

    }

    private fun setupObservers() {
        viewModel.poi.observe(this, Observer { poi ->
            activity_recorded_trip_poi_trip_name.text = poi.name
            activity_recorded_trip_poi_trip_description.text = poi.description
            activity_recorded_trip_poi_is_wp.isChecked = poi.isWaypoint
            activity_recorded_trip_poi_poi_type.text = poi.pointSubtype.name
        })
        viewModel.media.observe(this, Observer { mediaList ->
            mediaListManager.refreshPhotoGridAdapter(mediaList)
        })
    }

}
