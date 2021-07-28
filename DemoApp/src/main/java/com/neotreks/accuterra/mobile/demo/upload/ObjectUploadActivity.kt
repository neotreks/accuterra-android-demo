package com.neotreks.accuterra.mobile.demo.upload

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityObjectUploadBinding
import com.neotreks.accuterra.mobile.demo.extensions.isNotNullNorBlank
import com.neotreks.accuterra.mobile.demo.security.DemoAccessManager
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.demo.ui.OnListItemLongClickListener
import com.neotreks.accuterra.mobile.demo.util.DemoAppFileProvider
import com.neotreks.accuterra.mobile.demo.util.CrashSupport
import com.neotreks.accuterra.mobile.demo.util.MimeUtil
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.sync.model.UploadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Activity displaying upload request related to given object
 */
class ObjectUploadActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private val viewModel: ObjectUploadViewModel by viewModels()

    private lateinit var binding: ActivityObjectUploadBinding

    private lateinit var adapter: UploadRequestAdapter

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val TAG = "ObjectUploadActivity"

        private const val KEY_OBJECT_UUID = "OBJECT_ID"
        private const val KEY_VERSION_UUID = "VERSION_ID"

        fun createNavigateToIntent(context: Context, objectUUD: String, versionUuid: String): Intent {
            return Intent(context, ObjectUploadActivity::class.java)
                .apply {
                    putExtra(KEY_OBJECT_UUID, objectUUD)
                    putExtra(KEY_VERSION_UUID, versionUuid)
                }
        }

    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObjectUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        registerObservers()
        setupListView()

        parseIntent()

        lifecycleScope.launchWhenCreated {
            viewModel.uuid.value?.let { uuid ->
                displayProgressBar()
                viewModel.loadData(this@ObjectUploadActivity, uuid, viewModel.versionUuid.value)
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

        val objectUuid = intent.getStringExtra(KEY_OBJECT_UUID)
        val versionUuid = intent.getStringExtra(KEY_VERSION_UUID)

        require(objectUuid.isNotNullNorBlank()) { "No intent key $KEY_OBJECT_UUID provided." }

        viewModel.uuid.value = objectUuid
        viewModel.versionUuid.value = versionUuid
    }

    private fun setupToolbar() {

        setSupportActionBar(binding.activityObjectUploadToolbar.generalToolbar)
        binding.activityObjectUploadToolbar.generalToolbarTitle.text = getString(R.string.upload_resume_upload_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun registerObservers() {
        viewModel.uuid.observe(this, { uuid ->
            binding.activityObjectUploadTitleValue.text = uuid
        })
        viewModel.requests.observe(this, { requests ->
            Log.d(TAG, "Upload requests count: ${requests.size}")
            adapter.setItems(requests)
        })
        viewModel.workerInfo.observe(this, { info ->
            binding.activityObjectUploadWorkerStatus.text = info.joinToString { "State: ${it.state}, attempt count: ${it.attemptCount}" }
        })
    }

    private fun setupListView() {
        adapter = UploadRequestAdapter(this, mutableListOf())
        binding.activityObjectUploadList.adapter = adapter
        adapter.setOnListItemLongClickListener(object : OnListItemLongClickListener<UploadRequest> {
            override fun onListItemLongClick(item: UploadRequest, view: View) {
                onShareUpload(item)
            }
        })
        // Swipe listener
        binding.activityObjectUploadSwipeRefresh.setOnRefreshListener {
            onDoRefresh()
        }
        // Set `no data` view
        binding.activityObjectUploadList.emptyView = binding.activityObjectUploadListNoDataLabel
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

            // Worker info
            val workerStates = viewModel.workerInfo.value

            // Build full list of data
            val label = "Upload requests for: ${viewModel.uuid.value}"
            val data = listOf(label) + (workerStates?.map { it.toString() } ?: listOf())  + (infos ?: listOf())

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

    private fun onShareUpload(item: UploadRequest) {
        try {
            val path = item.dataPath
            if (path.isNullOrBlank()) {
                toast(getString(R.string.activity_upload_object_error_no_file))
                return
            }
            val file = File(path)
            if (!file.exists()) {
                toast(getString(R.string.activity_upload_object_error_file_not_exists, file.path))
                return
            }
            val uri = FileProvider.getUriForFile(
                applicationContext, DemoAppFileProvider.AUTHORITY, file
            )
            val mime = MimeUtil.getMimeType(file)
            if (mime == null) {
                toast(getString(R.string.activity_upload_object_error_getting_mime, file.path))
                return
            }
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.activity_upload_object_sharing_trip_file, item.dataUuid))
                putExtra(Intent.EXTRA_TEXT, item.toString())
                type = mime
            }
            shareIntent.clipData = ClipData.newUri(contentResolver, file.name, uri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(shareIntent, "Send to"))
        } catch (e: Exception) {
            Log.e(TAG, "Error while sharing a file: ${item.dataPath}", e)
            CrashSupport.reportError(e, "Error while sharing a file: ${item.dataPath}")
        }
    }

    private fun displayProgressBar() {
        binding.activityObjectUploadSwipeRefresh.isRefreshing = true
    }

    private fun hideProgressBar() {
        binding.activityObjectUploadSwipeRefresh.isRefreshing = false
    }

}