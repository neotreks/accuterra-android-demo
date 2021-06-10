package com.neotreks.accuterra.mobile.demo.offlinemap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityOfflineMapsBinding
import com.neotreks.accuterra.mobile.demo.offline.ApkOfflineCacheBackgroundService
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.cache.OfflineCacheBackgroundService
import com.neotreks.accuterra.mobile.sdk.map.cache.*
import java.lang.ref.WeakReference

class OfflineMapsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "OfflineMapsActivity"
        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, OfflineMapsActivity::class.java)
        }
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private var offlineCacheBgService: OfflineCacheBackgroundService? = null

    private val viewModel: OfflineMapsViewModel by viewModels()

    private lateinit var binding: ActivityOfflineMapsBinding

    private lateinit var activityOfflineMapsViewAdapter: ActivityOfflineMapsViewAdapter
    private val offlineMapsListener by lazy { OfflineMapListener(WeakReference(this)) }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfflineMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        displayProgressBar()
        registerViewModelObservers()
        setupToolbar()
        setupListView()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        ApkOfflineCacheBackgroundService.bindService(this, lifecycleScope, offlineCacheServiceConnection)
        // Cleanup setting
        setButtonsEnable(true)
    }


    override fun onStop() {
        Log.i(TAG, "onStop()")
        ApkOfflineCacheBackgroundService.unbindService(this, offlineCacheServiceConnection)
        super.onStop()
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

    // Used to map
    private val connectionListener = object: OfflineCacheServiceConnectionListener {
        override fun onConnected(service: OfflineCacheBackgroundService) {
            offlineCacheBgService = service
            lifecycleScope.launchWhenCreated {
                offlineCacheBgService?.offlineMapManager?.addProgressListener(cacheProgressListener)
                loadOfflineMaps(service.offlineMapManager)
            }
        }
        override fun onDisconnected() {
            offlineCacheBgService?.offlineMapManager?.removeProgressListener(cacheProgressListener)
            offlineCacheBgService = null
        }
    }

    private val cacheProgressListener = CacheProgressListener(this)

    private val offlineCacheServiceConnection = OfflineCacheBackgroundService.createServiceConnection(
        connectionListener
    )

    private fun setupToolbar() {

        setSupportActionBar(binding.activityOfflineMapsToolbar.generalToolbar)
        binding.activityOfflineMapsToolbar.generalToolbarTitle.text = getString(R.string.activity_offline_maps_title)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
    }

    private fun setupListView() {
        activityOfflineMapsViewAdapter = ActivityOfflineMapsViewAdapter(context = this,
            items = viewModel.listItems.value ?: mutableListOf(), listener = offlineMapsListener,
            lifecycleScope = lifecycleScope)
        binding.activityOfflineMapsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.activityOfflineMapsRecyclerView.adapter = activityOfflineMapsViewAdapter
        binding.activityOfflineMapsSwipeRefresh.setOnRefreshListener {
            offlineCacheBgService?.offlineMapManager?.let {
                loadOfflineMaps(it)
            }
        }
    }

    private fun setupButtons() {

        binding.activityOfflineMapsDownloadNewMapButton.setOnClickListener {
            setButtonsEnable(false)
            onDownloadNewMap()
        }

    }

    private fun onDownloadNewMap() {
        val intent = NewOfflineMapActivity.createNavigateToIntent(this)
        startActivity(intent)
    }

    private fun onEditOfflineMap(offlineMapId: String) {
        val intent = NewOfflineMapActivity.createNavigateToIntent(this, offlineMapId)
        startActivity(intent)
    }

    private fun setButtonsEnable(enabled: Boolean) {
        binding.activityOfflineMapsDownloadNewMapButton.isClickable = enabled
    }

    private fun registerViewModelObservers() {
        viewModel.listItems.observe(this, { offlineMaps ->
            hideProgressBar()
            activityOfflineMapsViewAdapter.setItems(offlineMaps)
            binding.activityOfflineMapsNoMapsLabel.visibility = offlineMaps.isEmpty().visibility
        })
    }

    private fun hideProgressBar() {
        binding.activityOfflineMapsSwipeRefresh.isRefreshing = false
    }

    private fun displayProgressBar() {
        binding.activityOfflineMapsSwipeRefresh.isRefreshing = true
    }

    private fun loadOfflineMaps(offlineMapManager: IOfflineMapManager) {
        displayProgressBar()
        viewModel.loadOfflineMaps(this, offlineMapManager)
    }

    private fun showRenameDialog(offlineMap: IAreaOfflineMap) {
        (offlineMap as? IAreaOfflineMap)?.let { _ ->

            DialogUtil.buildInputDialog(
                this,
                title = getString(R.string.activity_offline_maps_rename_title),
                positiveCode = { text ->
                    if (text.isNotBlank()) {
                        renameOfflineMap(offlineMap, text)
                    }
                },
                hint = getString(R.string.activity_offline_maps_rename_newname),
                positiveCodeLabel = getString(R.string.general_ok),
                negativeCodeLabel = getString(R.string.cancel)
            ).show()
        }
    }

    private fun showCancelPrompt(offlineMap: IOfflineMap) {
        DialogUtil.buildYesNoDialog(this,
            title = getString(R.string.activity_offline_maps_cancel_download_title),
            message = getString(R.string.activity_offline_maps_cancel_download_message,
                getOfflineMapName(offlineMap)),
            positiveCode = {
                cancelOrDeleteDownload(offlineMap)
            },
            positiveCodeLabel = getString(R.string.general_yes),
            negativeCodeLabel = getString(R.string.general_no)
        ).show()
    }

    private fun showDeletePrompt(offlineMap: IOfflineMap) {
        DialogUtil.buildYesNoDialog(this,
            title = getString(R.string.activity_offline_maps_delete_download_title),
            message = getString(R.string.activity_offline_maps_delete_download_message,
                getOfflineMapName(offlineMap)),
            positiveCode = {
                cancelOrDeleteDownload(offlineMap)
            },
            positiveCodeLabel = getString(R.string.general_yes),
            negativeCodeLabel = getString(R.string.general_no)
        ).show()
    }

    private fun getOfflineMapName(offlineMap: IOfflineMap) : String {
        return when (offlineMap) {
            is IAreaOfflineMap ->
                offlineMap.areaName
            is IOverlayOfflineMap ->
                "OVERLAY"
            is ITrailOfflineMap ->
                "TRAIL ${offlineMap.trailName}"
            else ->
                offlineMap.offlineMapId
        }
    }

    private fun renameOfflineMap(offlineMap: IAreaOfflineMap, newName: String) {
        lifecycleScope.launchWhenCreated {
            offlineCacheBgService?.offlineMapManager?.let { offlineMapManager ->
                offlineMapManager.renameAreaOfflineMap(offlineMap, newName)
                // reload offline maps
                loadOfflineMaps(offlineMapManager)
            }
        }
    }

    /**
     * Removes current offline map and creates it again
     */
    private fun updateOfflineMap(offlineMap: IOfflineMap) {
        lifecycleScope.launchWhenCreated {
            offlineCacheBgService?.offlineMapManager?.let { offlineMapManager ->
                // Delete current offline map
                offlineMapManager.updateOfflineMap(offlineMap)
                // reload offline maps
                loadOfflineMaps(offlineMapManager)
            }
        }
    }

    private fun cancelOrDeleteDownload(offlineMap: IOfflineMap) {
        lifecycleScope.launchWhenCreated {
            offlineCacheBgService?.offlineMapManager?.let { offlineMapManager ->
                offlineMapManager.deleteOfflineMap(offlineMap.offlineMapId)
                // reload offline maps
                loadOfflineMaps(offlineMapManager)
            }
        }
    }

    private fun showDownloadFailedError(error: HashMap<IOfflineResource, String>, offlineMap: IOfflineMap) {
        lifecycleScope.launchWhenCreated {
            val activity = this@OfflineMapsActivity
            val errorMessage = error.map { e ->
                "${e.key.getResourceTypeName()}: ${e.value}"
            }.joinToString("\n")
            DialogUtil.buildOkDialog(
                activity,
                activity.getString(R.string.download_failed),
                errorMessage
            ).show()
            activity.viewModel.updateItem(offlineMap)
            activity.activityOfflineMapsViewAdapter.notifyItemChanged(offlineMap)
        }
    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    class CacheProgressListener(activity: OfflineMapsActivity): ICacheProgressListener {

        private val weakActivity = WeakReference(activity)

        override fun onComplete(offlineMap: IOfflineMap) {
            weakActivity.get()?.let { activity ->
                activity.lifecycleScope.launchWhenCreated {
                    activity.viewModel.updateItem(offlineMap)
                    activity.activityOfflineMapsViewAdapter.notifyItemChanged(offlineMap)
                }
            }
        }

        override fun onComplete() {
            // We do not wan to do anything
        }

        override fun onError(error: HashMap<IOfflineResource, String>, offlineMap: IOfflineMap) {
            weakActivity.get()?.let { activity ->
                activity.lifecycleScope.launchWhenCreated {
                    if (offlineMap.status != OfflineMapStatus.CANCELED) {
                        activity.showDownloadFailedError(error, offlineMap)
                    }
                }
            }
        }

        override fun onProgressChanged(offlineMap: IOfflineMap) {
            weakActivity.get()?.let { activity ->
                activity.lifecycleScope.launchWhenCreated {
                    activity.viewModel.updateItem(offlineMap)
                    activity.activityOfflineMapsViewAdapter.notifyItemChanged(offlineMap)
                }
            }
        }
    }

    class OfflineMapListener(private val activityRef: WeakReference<OfflineMapsActivity>)
        : ActivityOfflineMapsViewHolder.Listener {
        override fun onItemClick(item: IOfflineMap) {
            // nothing to do
        }

        override fun onActionButtonClick(item: IOfflineMap, view: View) {
            activityRef.get()?.let {
                val popup = PopupMenu(it, view)
                popup.inflate(R.menu.menu_offline_maps)

                val updateMenu = popup.menu.findItem(R.id.activity_offline_maps_menu_update)
                val editMenu = popup.menu.findItem(R.id.activity_offline_maps_menu_edit)
                val renameMenu = popup.menu.findItem(R.id.activity_offline_maps_menu_rename)
                val deleteMenu = popup.menu.findItem(R.id.activity_offline_maps_menu_delete)
                val cancelMenu = popup.menu.findItem(R.id.activity_offline_maps_menu_cancel)
                val pauseMenu = popup.menu.findItem(R.id.activity_offline_maps_menu_pause)
                val resumeMenu = popup.menu.findItem(R.id.activity_offline_maps_menu_resume)

                // Check offline map state
                when (item.status) {
                    OfflineMapStatus.FAILED -> {
                        updateMenu.isVisible = false
                        editMenu.isVisible = false
                        renameMenu.isVisible = false
                        cancelMenu.isVisible = false
                        deleteMenu.isVisible = true
                        resumeMenu.isVisible = false
                        pauseMenu.isVisible = false
                    }
                    OfflineMapStatus.IN_PROGRESS -> {
                        updateMenu.isVisible = false
                        editMenu.isVisible = false
                        renameMenu.isVisible = true
                        cancelMenu.isVisible = true
                        deleteMenu.isVisible = false
                        resumeMenu.isVisible = false
                        pauseMenu.isVisible = true
                    }
                    OfflineMapStatus.COMPLETE -> {
                        updateMenu.isVisible = true
                        editMenu.isVisible = item.type == OfflineMapType.AREA
                        renameMenu.isVisible = item.type == OfflineMapType.AREA
                        cancelMenu.isVisible = false
                        deleteMenu.isVisible = true
                        resumeMenu.isVisible = false
                        pauseMenu.isVisible = false
                    }
                    OfflineMapStatus.WAITING -> {
                        updateMenu.isVisible = false
                        editMenu.isVisible = false
                        renameMenu.isVisible = false
                        cancelMenu.isVisible = true
                        deleteMenu.isVisible = false
                        resumeMenu.isVisible = false
                        pauseMenu.isVisible = false
                    }
                    OfflineMapStatus.NOT_CACHED -> {
                        updateMenu.isVisible = false
                        editMenu.isVisible = false
                        renameMenu.isVisible = false
                        cancelMenu.isVisible = true
                        deleteMenu.isVisible = false
                        resumeMenu.isVisible = false
                        pauseMenu.isVisible = false
                    }
                    OfflineMapStatus.PAUSED -> {
                        updateMenu.isVisible = false
                        editMenu.isVisible = false
                        renameMenu.isVisible = false
                        cancelMenu.isVisible = true
                        deleteMenu.isVisible = false
                        resumeMenu.isVisible = true
                        pauseMenu.isVisible = false
                    }
                    OfflineMapStatus.CANCELED -> {
                    }
                }

                popup.setOnMenuItemClickListener { popupItem ->
                    when (popupItem.itemId) {
                        R.id.activity_offline_maps_menu_update -> {
                            it.updateOfflineMap(item)
                            true
                        }
                        R.id.activity_offline_maps_menu_edit -> {
                            it.onEditOfflineMap(item.offlineMapId)
                            true
                        }
                        R.id.activity_offline_maps_menu_rename -> {
                            (item as? IAreaOfflineMap)?.let { areaOfflineMap ->
                                it.showRenameDialog(areaOfflineMap)
                            }
                            true
                        }
                        R.id.activity_offline_maps_menu_cancel -> {
                            it.showCancelPrompt(item)
                            true
                        }
                        R.id.activity_offline_maps_menu_delete -> {
                            it.showDeletePrompt(item)
                            true
                        }
                        R.id.activity_offline_maps_menu_resume -> {
                            it.offlineCacheBgService?.offlineMapManager?.let { offlineMapManager ->
                                it.lifecycleScope.launchWhenCreated {
                                    offlineMapManager.resume()
                                }
                            }
                            true
                        }
                        R.id.activity_offline_maps_menu_pause -> {
                            it.offlineCacheBgService?.offlineMapManager?.let { offlineMapManager ->
                                it.lifecycleScope.launchWhenCreated {
                                    offlineMapManager.pause()
                                }
                            }
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }
}
