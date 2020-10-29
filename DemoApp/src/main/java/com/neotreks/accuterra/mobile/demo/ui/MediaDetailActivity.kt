package com.neotreks.accuterra.mobile.demo.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.extensions.isNotNullNorBlank
import com.neotreks.accuterra.mobile.demo.util.visibility
import kotlinx.android.synthetic.main.activity_media_detail.*
import kotlinx.android.synthetic.main.general_toolbar.*

class MediaDetailActivity : AppCompatActivity() {

    private lateinit var viewModel: MediaDetailViewModel

    companion object {

        private const val TAG = "MediaDetailActivity"

        private const val KEY_UUID = "KEY_UUID"
        private const val KEY_URI = "KEY_URI"
        private const val KEY_URL = "KEY_URL"

        /**
         * Need to provide [uri] or [mediaUuid] (only for already saved trip medias)
         */
        fun createNavigateToIntent(context: Context,
                                   mediaUuid: String? = null,
                                   uri: Uri? = null,
                                   url: String? = null): Intent {
            return Intent(context, MediaDetailActivity::class.java)
                .apply {
                    putExtra(KEY_UUID, mediaUuid)
                    putExtra(KEY_URI, uri)
                    putExtra(KEY_URL, url)
                }
        }
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_detail)
        // Init view model
        viewModel = ViewModelProvider(this).get(MediaDetailViewModel::class.java)

        // Setup UI
        setupToolbar()
        setupObservers()

        // Parse intent and load data into view model
        parseIntent()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun setupToolbar() {

        setSupportActionBar(general_toolbar)
        general_toolbar_title.text = getString(R.string.general_media)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun setupObservers() {

        viewModel.uri.observe(this, Observer { uri ->
            if (uri == null) {
                return@Observer
            }
            val image = activity_media_detail_image
            // Load
            val options = UiUtils.getDefaultImageOptions()
            Glide.with(this@MediaDetailActivity)
                .applyDefaultRequestOptions(options)
                .load(uri)
                .into(image)
            // Hide the progress bar
            activity_media_detail_spinner.visibility = false.visibility
        })

    }

    private fun parseIntent() {
        // Parse UUID
        val uuid = intent.getStringExtra(KEY_UUID)
        val url = intent.getStringExtra(KEY_URL)
        val uri = if (intent.hasExtra(KEY_URI)) {
            intent.getParcelableExtra<Uri>(KEY_URI)
        } else {
            null
        }

        check(uuid.isNotNullNorBlank() || uri != null || url.isNotNullNorBlank()) {
            "The $KEY_UUID, $KEY_URI or $KEY_URL must be provided"
        }

        when {
            uri != null -> {
                // Using this for TRIP media which are not "saved" yet
                // e.g. for new media for new POI
                Log.i(TAG, "Media uri = $uri")
                viewModel.loadMediaFromUri(uri)
            }
            uuid.isNotNullNorBlank() -> {
                // Using this for TRIP media which are already saved
                // e.g. when the POI is saved into SDK
                Log.i(TAG, "Media uuid = $uuid")
                viewModel.loadMediaFromUuid(uuid!!, this)
            }
            url.isNotNullNorBlank() -> {
                // Using this for TRAIL media which are defined by URL
                Log.i(TAG, "Media url = $url")
                viewModel.loadMediaFromUrl(url!!, this)
            }
        }
    }

}
