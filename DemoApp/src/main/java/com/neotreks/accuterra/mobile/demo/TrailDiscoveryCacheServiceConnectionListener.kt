package com.neotreks.accuterra.mobile.demo

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.sdk.map.cache.IAreaOfflineMap
import com.neotreks.accuterra.mobile.sdk.map.cache.ICacheProgressListener
import com.neotreks.accuterra.mobile.sdk.map.cache.IOfflineMap
import com.neotreks.accuterra.mobile.sdk.map.cache.IOfflineResource
import com.neotreks.accuterra.mobile.sdk.map.cache.ITrailOfflineMap
import java.lang.ref.WeakReference

/**
 * Trail Discovery Activity Cache Service Connection Listener
 */
class TrailDiscoveryCacheServiceConnectionListener(activity: TrailDiscoveryActivity) : ICacheProgressListener {

    private val weakActivity = WeakReference(activity)

    override fun onComplete(offlineMap: IOfflineMap) {
        weakActivity.get()?.let { activity ->
            activity.lifecycleScope.launchWhenCreated {
                activity.binding.activityTrailOfflinemapProgressLayout.visibility = View.GONE
            }
        }
    }

    override fun onComplete() {
        // We do not wan to do anything
    }

    override fun onError(error: HashMap<IOfflineResource, String>, offlineMap: IOfflineMap) {
        weakActivity.get()?.let { activity ->
            activity.lifecycleScope.launchWhenCreated {
                val errorMessage = error.map { e ->
                    "${e.key.getResourceTypeName()}: ${e.value}"
                }.joinToString("\n")
                DialogUtil.buildOkDialog(activity, activity.getString(R.string.download_failed),errorMessage).show()
            }
        }
    }

    override fun onProgressChanged(offlineMap: IOfflineMap) {
        weakActivity.get()?.let { activity ->
            activity.lifecycleScope.launchWhenCreated {
                // Show what is downloading and progress
                val progressPercents = (100.0 * offlineMap.progress).toInt()
                activity.binding.activityTrailOfflinemapProgressLayout.visibility = View.VISIBLE
                activity.binding.activityTrailOfflinemapProgressBar.progress = progressPercents

                var cacheName = offlineMap.type.name
                when (offlineMap) {
                    is ITrailOfflineMap ->
                        cacheName += "(${offlineMap.trailId})"
                    is IAreaOfflineMap ->
                        cacheName += "(${offlineMap.areaName})"
                }

                activity.binding.activityTrailOfflinemapProgressLabel.text = activity.getString(R.string.downloading_cache, cacheName, progressPercents)
            }
        }
    }

    override fun onImageryDeleted(offlineMaps: List<IOfflineMap>) {
        // We do not want to show anything now
    }

}