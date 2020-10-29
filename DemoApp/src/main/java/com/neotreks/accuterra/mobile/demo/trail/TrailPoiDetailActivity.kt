package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.ui.MediaDetailActivity
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailMedia
import kotlinx.android.synthetic.main.activity_trail_poi_detail.*
import kotlinx.android.synthetic.main.general_toolbar.*

class TrailPoiDetailActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val viewModel: TrailPoiDetailViewModel by viewModels()

    private lateinit var mediaListManager: TrailMediaListManager

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val KEY_POI_ID = "POI_ID"

        fun createNavigateToIntent(context: Context, poiId: Long? = null): Intent {
            return Intent(context, TrailPoiDetailActivity::class.java)
                .apply {
                    putExtra(KEY_POI_ID, poiId)
                }
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trail_poi_detail)

        // Setup toolbar
        setupToolbar()

        // Setup UI for media
        setupPhotoGrid()

        // Register observers
        registerViewModelObserver()

        // Load the poi
        parseIntent()
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

    private fun parseIntent() {

        val pointId = intent.getLongExtra(KEY_POI_ID, -1)
        require(pointId != -1L) { "Invalid $KEY_POI_ID value." }

        viewModel.loadPoi(this, pointId)
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

    private fun registerViewModelObserver() {
        viewModel.poi.observe(this, Observer { poi ->
            if (poi == null) {
                return@Observer
            }
            // Load data into the UI
            activity_trail_poi_detail_name.text = poi.point.name
            activity_trail_poi_detail_description.text = poi.description
            activity_trail_poi_detail_poi_type.text = poi.point.subType.name
            activity_trail_poi_detail_is_wp.isChecked = poi.point.isWaypoint ?: false
            // Attach medias
            mediaListManager.refreshPhotoGridAdapter(poi.point.media)
        })
    }

    private fun setupPhotoGrid() {
        mediaListManager = TrailMediaListManager(
            this,
            lifecycleScope,
            activity_recorded_trip_poi_photos,
            object : TrailMediaListManager.MediaListClickListener {
                override fun onItemClicked(media: TrailMedia) {
                    val intent = MediaDetailActivity.createNavigateToIntent(
                        context = this@TrailPoiDetailActivity,
                        url = media.url
                    )
                    startActivity(intent)
                }

                override fun onDeleteItemClicked(media: TrailMedia) {
                    throw IllegalStateException("Media deletion not supported.")
                }
            }, readOnly = true
        )
        mediaListManager.setupPhotoGrid()
    }

}