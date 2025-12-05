package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.TrailDiscoveryActivity
import com.neotreks.accuterra.mobile.demo.databinding.ActivityNewTripBinding
import com.neotreks.accuterra.mobile.demo.extensions.applyAllWindowInsetsButStatusBar
import com.neotreks.accuterra.mobile.demo.extensions.applyStatusBarWindowInsets
import com.neotreks.accuterra.mobile.demo.trip.trailcollection.TrailCollectionActivity

class NewTripActivity : AppCompatActivity() {

    companion object {
        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, NewTripActivity::class.java)
        }
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: ActivityNewTripBinding

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewTripBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyAllWindowInsetsButStatusBar(binding.root)
        applyStatusBarWindowInsets(binding.newTripActivityToolbar.root)

        setupToolbar()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        // Cleanup setting
        setButtonsEnable(true)
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

        setSupportActionBar(binding.newTripActivityToolbar.generalToolbar)
        binding.newTripActivityToolbar.generalToolbarTitle.text = getString(R.string.activity_new_trip_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun setupButtons() {

        binding.newTripActivityChoseRouteList.setOnClickListener {
            setButtonsEnable(false)
            onChooseRoute()
        }

        binding.newTripActivityFreeRoamList.setOnClickListener {
            setButtonsEnable(false)
            onFreeRoam()
        }

        binding.newTripActivityTrailCollectionList.setOnClickListener {
            setButtonsEnable(false)
            onTrailCollection()
        }

    }

    private fun onFreeRoam() {
        val intent = TripRecordingActivity.createNavigateToIntent(this)
        startActivity(intent)
        finish()
    }

    private fun onChooseRoute() {
        val intent = TrailDiscoveryActivity.createNavigateToIntent(this)
        startActivity(intent)
        finish()
    }

    private fun onTrailCollection() {
        val intent = TrailCollectionActivity.createNavigateToIntent(this)
        startActivity(intent)
        finish()
    }

    private fun setButtonsEnable(enabled: Boolean) {
        binding.newTripActivityChoseRouteList.isClickable = enabled
        binding.newTripActivityFreeRoamList.isClickable = enabled
        binding.newTripActivityTrailCollectionList.isClickable = enabled
    }

}
