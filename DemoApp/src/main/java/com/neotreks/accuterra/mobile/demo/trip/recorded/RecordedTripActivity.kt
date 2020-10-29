package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.extensions.toLocalDateString
import com.neotreks.accuterra.mobile.demo.longToast
import kotlinx.android.synthetic.main.activity_recorded_trip.*

/**
 * Recorded trip detail view
 */
class RecordedTripActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: RecordedTripViewModel

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        const val KEY_TRIP_ID = "KEY_TRIP_ID"

        const val RESULT_NOT_FOUND = 103

        fun createNavigateToIntent(context: Context, tripUuid: String): Intent {
            return Intent(context, RecordedTripActivity::class.java)
                .apply {
                    putExtra(KEY_TRIP_ID, tripUuid)
                }
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recorded_trip)
        // Init view model
        viewModel = ViewModelProvider(this).get(RecordedTripViewModel::class.java)

        registerViewModelObserver()
        setupFragments()

        parseIntent()
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun setupFragments() {
        activity_recorded_trip_view_pager.adapter = RecordedTripViewPageAdapter(
            supportFragmentManager,
            this
        )
    }

    private fun parseIntent() {
        // Parse + set the trail ID
        require(intent.hasExtra(KEY_TRIP_ID)) { "No intent key $KEY_TRIP_ID provided." }
        val uuid = intent.getStringExtra(KEY_TRIP_ID)!!

        lifecycleScope.launchWhenCreated {
            viewModel.loadTrip(uuid, applicationContext)
        }
    }

    private fun registerViewModelObserver() {
        viewModel.trip.observe(this, Observer { trip ->
            if (trip == null) {
                longToast(getString(R.string.activity_recorded_trip_trip_not_found))
                setResult(RESULT_NOT_FOUND)
                finish()
                return@Observer
            }
            activity_recorded_trip_name.text = trip.tripInfo.name
            activity_recorded_trip_date.text = trip.recordingInfo.start.toLocalDateString()
        })
    }

}