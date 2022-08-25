package com.neotreks.accuterra.mobile.demo.offlinemap

import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.OfflineMapListItemBinding
import com.neotreks.accuterra.mobile.demo.extensions.bytesToHumanReadable
import com.neotreks.accuterra.mobile.sdk.map.cache.*

/**
 * View binder for the Offline Map item
 */
class ActivityOfflineMapsViewBinder(val listener: ActivityOfflineMapsViewHolder.Listener) {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {
        const val viewResourceId = R.layout.offline_map_list_item
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    fun getViewResourceId(): Int {
        return R.layout.offline_map_list_item
    }

    fun bindView(view: View, item: IOfflineMap) {
        val binding = OfflineMapListItemBinding.bind(view)

        // Set listeners
        view.setOnClickListener { listener.onItemClick(item) }
        binding.offlineMapListItemActionButton.setOnClickListener {
            listener.onActionButtonClick(item, binding.offlineMapListItemActionButton)
        }

        var size = "${(item.size ?: 0).bytesToHumanReadable()}"

        var name = ""
        val styles = if (item.containsImagery) {
            "AccuTerra Outdoors - Imagery"
        } else {
            "AccuTerra Outdoors"
        }

        val description = when (item.status) {
            OfflineMapStatus.COMPLETE ->
                "$size - $styles"
            OfflineMapStatus.FAILED ->
                view.context.getString(R.string.download_failed)
            OfflineMapStatus.PAUSED ->
                view.context.getString(R.string.download_paused)
            OfflineMapStatus.WAITING ->
                view.context.getString(R.string.download_waiting)
            else ->
                view.context.getString(
                    R.string.downloading_percents,
                    (item.progress * 100).toInt()
                )
        }

        when(item) {
            is IAreaOfflineMap -> {
                name = item.areaName
            }
            is IOverlayOfflineMap -> {
                name ="OVERLAY"
            }
            is ITrailOfflineMap -> {
                name ="TRAIL ${item.trailName}"
            }
        }

        // Name and description
        binding.offlineMapListItemName.text = name
        binding.offlineMapListItemDescription.text = description
    }
}
