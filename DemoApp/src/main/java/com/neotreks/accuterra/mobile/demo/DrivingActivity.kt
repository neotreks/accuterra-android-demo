package com.neotreks.accuterra.mobile.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.neotreks.accuterra.mobile.demo.trail.OnListItemClickedListener
import com.neotreks.accuterra.mobile.demo.trail.PoiListAdapter
import com.neotreks.accuterra.mobile.demo.util.ListViewSize
import com.neotreks.accuterra.mobile.demo.util.drawDownArrowOnLeftSide
import com.neotreks.accuterra.mobile.demo.util.drawUpArrowOnLeftSide
import com.neotreks.accuterra.mobile.sdk.trail.model.MapPoint
import com.neotreks.accuterra.mobile.sdk.trail.model.Trail
import kotlinx.android.synthetic.main.activity_driving.*

class DrivingActivity : AppCompatActivity() {

    companion object {

        private const val TAG = "DrivingActivity"

        private const val KEY_TRAIL_ID = "KEY_TRAIL_ID"

        fun createNavigateToIntent(context: Context, trailId: Long): Intent {
            return Intent(context, DrivingActivity::class.java)
                .apply {
                    putExtra(KEY_TRAIL_ID, trailId)
                }
        }
    }

    private lateinit var viewModel: DrivingViewModel

    private lateinit var poiListAdapter: PoiListAdapter

    private var bottomListSize = ListViewSize.MEDIUM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driving)

        viewModel = ViewModelProvider(this).get(DrivingViewModel::class.java)
        parseIntent()

        viewModel.trail.observe(this, Observer { trail ->
            if (trail != null) {
                onTrailDataLoaded(trail)
            }
        })

        activity_driving_list.emptyView = activity_driving_empty_list_label
        setupButtons()
    }

    private fun onTrailDataLoaded(trail: Trail) {
        Log.d(TAG, trail.navigationInfo.mapPoints.toString())
        poiListAdapter =  PoiListAdapter(this, trail.navigationInfo.waypoints)
        activity_driving_list.adapter = poiListAdapter
        poiListAdapter.setOnListItemClickedListener(listener = object :OnListItemClickedListener<MapPoint> {
            override fun onListItemClicked(item: MapPoint) {
                selectPoi(item)
            }
        })
        hideProgressBar()
    }

    private fun parseIntent() {
        // Parse + set the trail ID
        require(intent.hasExtra(KEY_TRAIL_ID)) { "No intent key $KEY_TRAIL_ID provided." }
        val trailId = intent.getLongExtra(KEY_TRAIL_ID, Long.MIN_VALUE)
        viewModel.setTrailId(trailId, this)
    }

    private fun selectPoi(mapPoint: MapPoint?) {
        val poiId = if (viewModel.selectedPoiId == mapPoint?.id) {
            null
        } else {
            mapPoint?.id
        }
        viewModel.selectedPoiId = poiId
        poiListAdapter.selectItem(poiId)
    }

    private fun toggleListView() {
        bottomListSize = bottomListSize.toggleListView()

        moveListViewGuideLine()
    }

    private fun setupButtons() {
        activity_driving_show_list_button.drawUpArrowOnLeftSide()
        activity_driving_show_list_button.setOnClickListener {
            toggleListView()
        }
    }

    private fun moveListViewGuideLine() {
        when(bottomListSize) {
            ListViewSize.MINIMUM -> {
                activity_driving_list_top_guideline.setGuidelinePercent(
                    //make only the activity_driving_main_view visible
                    1.0f - activity_driving_show_list_button.height.toFloat() / activity_driving_main_view.height.toFloat()
                )
                activity_driving_show_list_button.drawUpArrowOnLeftSide()
            }
            ListViewSize.MEDIUM -> {
                activity_driving_list_top_guideline.setGuidelinePercent(0.5f)
                activity_driving_show_list_button.drawUpArrowOnLeftSide()
            }
            ListViewSize.MAXIMUM -> {
                activity_driving_list_top_guideline.setGuidelinePercent(0.0f)
                activity_driving_show_list_button.drawDownArrowOnLeftSide()
            }
        }
    }

    @UiThread
    private fun hideProgressBar() {
        activity_driving_list_loading_progress.visibility = View.GONE
    }
}
