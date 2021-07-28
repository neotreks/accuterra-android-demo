package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityRecordedTripPoiBinding
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia

class RecordedTripPoiActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val viewModel: RecordedTripPoiViewModel by viewModels()

    private lateinit var mediaListManager: TripRecordingMediaListManager

    private lateinit var binding: ActivityRecordedTripPoiBinding

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

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
        binding = ActivityRecordedTripPoiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mediaListManager = TripRecordingMediaListManager(this, lifecycleScope,
            binding.activityRecordedTripPoiPhotos,
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

        val toolbar = binding.activityRecordedTripPoiToolbar
        setSupportActionBar(toolbar.actionToolbar)
        toolbar.actionToolbarTitle.text = getString(R.string.activity_recorded_trip_poi_title)
        toolbar.actionToolbarAction.text = ""

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

    }

    private fun setupObservers() {
        viewModel.poi.observe(this, { poi ->
            binding.activityRecordedTripPoiTripName.text = poi.name
            binding.activityRecordedTripPoiTripDescription.text = poi.description
            binding.activityRecordedTripPoiPoiType.text = poi.pointType.name
        })
        viewModel.media.observe(this, { mediaList ->
            mediaListManager.refreshPhotoGridAdapter(mediaList)
        })
    }

}
