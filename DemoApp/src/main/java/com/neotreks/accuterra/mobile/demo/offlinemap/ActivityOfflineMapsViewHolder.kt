package com.neotreks.accuterra.mobile.demo.offlinemap

import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.neotreks.accuterra.mobile.sdk.map.cache.IOfflineMap

/**
 * View holder for the [ActivityOfflineMapsViewAdapter]
 */
class ActivityOfflineMapsViewHolder(
    private val view: View,
    context: Context,
    listener: Listener,
    lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.ViewHolder(view) {

    private val areaOfflineMapViewBinder = ActivityOfflineMapsViewBinder(listener)

    fun bindView(item: IOfflineMap) {
        areaOfflineMapViewBinder.bindView(view, item)
    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    interface Listener {
        fun onItemClick(item: IOfflineMap)
        fun onActionButtonClick(item: IOfflineMap, view: View)
    }

}