package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityRecordedTripBinding
import com.neotreks.accuterra.mobile.demo.extensions.toLocalDateString
import com.neotreks.accuterra.mobile.demo.longToast

/**
 * Recorded trip detail view
 */
class RecordedTripActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var viewModel: RecordedTripViewModel

    private lateinit var binding: ActivityRecordedTripBinding

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
        binding = ActivityRecordedTripBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Init view model
        viewModel = ViewModelProvider(this).get(RecordedTripViewModel::class.java)
        setupToolbar()

        registerViewModelObserver()
        setupFragments()

        parseIntent()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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

        setSupportActionBar(binding.activityRecordedTripToolbar.generalToolbar)
        binding.activityRecordedTripToolbar.generalToolbarTitle.text = getString(R.string.general_trip)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }

    }

    private fun setupFragments() {
        binding.activityRecordedTripViewPager.adapter = RecordedTripViewPageAdapter(
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
            binding.activityRecordedTripName.text = trip.tripInfo.name
            binding.activityRecordedTripDate.text = trip.recordingInfo.start.toLocalDateString()
        })
    }

}