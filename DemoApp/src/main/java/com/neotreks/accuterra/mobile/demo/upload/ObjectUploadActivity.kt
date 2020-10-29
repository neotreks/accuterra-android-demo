package com.neotreks.accuterra.mobile.demo.upload

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.security.DemoAccessManager
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import kotlinx.android.synthetic.main.activity_object_upload.*
import kotlinx.android.synthetic.main.general_toolbar.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Activity displaying upload request related to given object
 */
class ObjectUploadActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val viewModel: ObjectUploadViewModel by viewModels()

    private lateinit var adapter: UploadRequestAdapter

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val TAG = "ObjectUploadActivity"

        private const val KEY_OBJECT_UUID = "OBJECT_ID"

        fun createNavigateToIntent(context: Context, objectUUD: String): Intent {
            return Intent(context, ObjectUploadActivity::class.java)
                .apply {
                    putExtra(KEY_OBJECT_UUID, objectUUD)
                }
        }

    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_upload)

        setupToolbar()
        registerObservers()
        setupListView()

        parseIntent()

        lifecycleScope.launchWhenCreated {
            viewModel.uuid.value?.let { uuid ->
                displayProgressBar()
                viewModel.loadUploadRequests(this@ObjectUploadActivity, uuid)
                hideProgressBar()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menu?.let {
            menuInflater.inflate(R.menu.menu_upload_object, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.upload_object_menu_item_refresh -> {
                lifecycleScope.launchWhenCreated {
                    onDoRefresh()
                }
                return true
            }
            R.id.upload_object_menu_item_copy -> {
                lifecycleScope.launchWhenCreated {
                    onCopyToClipboard()
                }
                return true
            }
            R.id.upload_object_menu_item_resume_queue -> {
                lifecycleScope.launchWhenCreated {
                    onResumeUploadQueue()
                }
                return true
            }
            R.id.upload_object_menu_item_reset_access_token -> {
                lifecycleScope.launchWhenCreated {
                    onResetAccessToken()
                }
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

    private fun parseIntent() {
        require(intent.hasExtra(KEY_OBJECT_UUID)) { "No intent key $KEY_OBJECT_UUID provided." }

        viewModel.uuid.value = intent.getStringExtra(KEY_OBJECT_UUID)
        require(viewModel.uuid.value != null) { "Invalid $KEY_OBJECT_UUID value." }
    }

    private fun setupToolbar() {

        setSupportActionBar(general_toolbar)
        general_toolbar_title.text = getString(R.string.upload_resume_upload_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun registerObservers() {
        viewModel.uuid.observe(this, Observer { uuid ->
            activity_object_upload_title_value.text = uuid
        })
        viewModel.requests.observe(this, Observer { requests ->
            Log.d(TAG, "Upload requests count: ${requests.size}")
            adapter.setItems(requests)
        })
    }

    private fun setupListView() {
        adapter = UploadRequestAdapter(this, mutableListOf())
        activity_object_upload_list.adapter = adapter
        // Swipe listener
        activity_object_upload_swipe_refresh.setOnRefreshListener {
            onDoRefresh()
        }
        // Set `no data` view
        activity_object_upload_list.emptyView = activity_object_upload_list_no_data_label
    }

    private fun onDoRefresh() {
        lifecycleScope.launchWhenCreated {
            displayProgressBar()
            viewModel.refresh(this@ObjectUploadActivity)
            hideProgressBar()
        }
    }

    private suspend fun onCopyToClipboard() {

        withContext(Dispatchers.Default) {

            // Build infos
            val infos = viewModel.requests.value?.map { request ->
                UploadRequestViewBinder.buildFullInfo(request)
            }

            // Build full list of data
            val label = "Upload requests for: ${viewModel.uuid.value}"
            val data = listOf(label) + (infos ?: listOf())

            // Gets a handle to the clipboard service.
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            // Creates a new text clip to put on the clipboard
            val clip: ClipData = ClipData.newPlainText(label, data.joinToString(separator = "\n\n"))

            // Put into the clipboard
            clipboard.setPrimaryClip(clip)

            // Display message
            withContext(Dispatchers.Main) {
                toast(getString(R.string.general_copied_to_clipboard))
            }
        }

    }

    private fun onResumeUploadQueue() {
        lifecycleScope.launchWhenCreated {
            val service = ServiceFactory.getSynchronizationService(this@ObjectUploadActivity)
            service.resumeUploadQueue(forceResume = true)
            toast(getString(R.string.upload_upload_queue_resumed))
        }
    }

    private fun onResetAccessToken() {
        lifecycleScope.launchWhenCreated {
            try {
                DemoAccessManager().resetToken(this@ObjectUploadActivity)
                toast(getString(R.string.upload_reset_access_token_done))
            } catch (e: Exception) {
                this@ObjectUploadActivity.toast(getString(R.string.upload_reset_access_token_failed, e.localizedMessage))
            }
        }
    }

    private fun displayProgressBar() {
        activity_object_upload_swipe_refresh.isRefreshing = true
    }

    private fun hideProgressBar() {
        activity_object_upload_swipe_refresh.isRefreshing = false
    }

}