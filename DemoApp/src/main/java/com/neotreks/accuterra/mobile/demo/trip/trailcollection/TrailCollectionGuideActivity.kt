package com.neotreks.accuterra.mobile.demo.trip.trailcollection

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityTrailCollectionGuideBinding
import com.neotreks.accuterra.mobile.demo.extensions.applyAllWindowInsetsButStatusBar
import com.neotreks.accuterra.mobile.demo.extensions.applyStatusBarWindowInsets

class TrailCollectionGuideActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var binding: ActivityTrailCollectionGuideBinding

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, TrailCollectionGuideActivity::class.java)
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrailCollectionGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyAllWindowInsetsButStatusBar(binding.root)
        applyStatusBarWindowInsets(binding.activityTrailCollectionGuideToolbar.root)

        setupToolbar()

        val html = getString(R.string.activity_trail_collection_guide_description)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            binding.activityTrailCollectionGuideDescription.text =
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            binding.activityTrailCollectionGuideDescription.text = Html.fromHtml(html)
        }
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun setupToolbar() {
        val toolbar = binding.activityTrailCollectionGuideToolbar
        setSupportActionBar(toolbar.trailCollectionToolbar)
        toolbar.trailCollectionToolbarTitle.text = getString(R.string.activity_trail_collection_description_toolbar_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

}