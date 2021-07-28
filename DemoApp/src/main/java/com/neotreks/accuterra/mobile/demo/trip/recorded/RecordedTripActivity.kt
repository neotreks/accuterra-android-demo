package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityRecordedTripBinding
import com.neotreks.accuterra.mobile.demo.extensions.toLocalDateString
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.demo.user.DemoIdentityManager
import com.neotreks.accuterra.mobile.demo.util.CrashSupport
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import kotlinx.coroutines.runBlocking

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        if (menu != null) {
            menuInflater.inflate(R.menu.menu_trip_recorded, menu)
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (menu != null) {
            val isCurrentUserTrip = isCurrentUserTrip()
            menu.findItem(R.id.trip_recorded_menu_item_trip_edit).isEnabled  = isCurrentUserTrip
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.trip_recorded_menu_item_trip_edit -> {
                onEditTrip()
                return true
            }
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

    private fun isCurrentUserTrip(): Boolean {
        val tripUserUuid = viewModel.trip.value?.userInfo?.driverId
        val currentUserUuid = runBlocking { DemoIdentityManager().getUserId(this@RecordedTripActivity) }
        return currentUserUuid == tripUserUuid
    }

    private fun onEditTrip() {
        DialogUtil.buildYesNoDialog(
            context = this,
            title = getString(R.string.activity_recorded_trip_edit_trip_title),
            message = getString(R.string.activity_recorded_trip_edit_trip_message),
            positiveCodeLabel = getString(R.string.general_edit),
            negativeCodeLabel = getString(R.string.general_cancel),
            positiveCode = {
                editTrip()
            }
        ).show()
    }

    private fun editTrip() {
        val dialog = DialogUtil.buildBlockingProgressDialog(
            context = this,
            title = getString(R.string.activity_recorded_trip_editing_trip)
        )
        dialog.show()
        lifecycleScope.launchWhenCreated {
            val service = ServiceFactory.getTripRecordingService(this@RecordedTripActivity)
            val trip = viewModel.trip.value
            if (trip == null) {
                toast(getString(R.string.activity_recorded_trip_edit_trip_is_null))
            } else {
                // Start trip editing - this will handle it based on passed `TripRecording`
                val result = service.startTripEditing(trip)
                dialog.dismiss()
                // Check if we were able to start editing of the `TripRecording`
                if (result.isSuccess) {
                    // We were able to start editing
                    toast(getString(R.string.activity_recorded_trip_edit_success))
                    // Navigate to trip editing screen
                    val intent = TripSaveActivity.createNavigateToIntent(this@RecordedTripActivity, trip.tripInfo.uuid)
                    startActivity(intent)
                    // Close the screen
                    finish()
                } else {
                    // We were not able to start trip editing
                    val resultValue = result.value
                    longToast(getString(R.string.activity_recorded_trip_edit_failed, resultValue?.name ?: "Unknown"))
                    CrashSupport.reportError(result, "Trip UUID: ${viewModel.trip.value?.tripInfo}")
                }
            }
        }
    }

}