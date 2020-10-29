package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.TrailDiscoveryActivity
import kotlinx.android.synthetic.main.activity_new_trip.*
import kotlinx.android.synthetic.main.general_toolbar.*

class NewTripActivity : AppCompatActivity() {

    companion object {
        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, NewTripActivity::class.java)
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_trip)

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

        setSupportActionBar(general_toolbar)
        general_toolbar_title.text = getString(R.string.activity_new_trip_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun setupButtons() {
        new_trip_activity_chose_route.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        new_trip_activity_free_roam.paintFlags = Paint.UNDERLINE_TEXT_FLAG

        new_trip_activity_chose_route.setOnClickListener {
            setButtonsEnable(false)
            onChooseRoute()
        }

        new_trip_activity_free_roam.setOnClickListener {
            setButtonsEnable(false)
            onFreeRoam()
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

    private fun setButtonsEnable(enabled: Boolean) {
        new_trip_activity_chose_route.isClickable = enabled
        new_trip_activity_free_roam.isClickable = enabled
    }

}
