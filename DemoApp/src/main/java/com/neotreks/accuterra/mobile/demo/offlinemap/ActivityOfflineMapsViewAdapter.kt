package com.neotreks.accuterra.mobile.demo.offlinemap

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.neotreks.accuterra.mobile.sdk.map.cache.IOfflineMap

/**
 * Adapter for list of offline maps
 */
class ActivityOfflineMapsViewAdapter(
    private val context: Context,
    private var items: MutableList<IOfflineMap>,
    private val listener: ActivityOfflineMapsViewHolder.Listener,
    private val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.Adapter<ActivityOfflineMapsViewHolder>() {

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    init {
        setHasStableIds(false)
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityOfflineMapsViewHolder {
        val resourceId = ActivityOfflineMapsViewBinder.viewResourceId
        val view = LayoutInflater.from(parent.context).inflate(resourceId, parent, false)

        return ActivityOfflineMapsViewHolder(view, context, listener, lifecycleScope)
    }

    override fun onBindViewHolder(holder: ActivityOfflineMapsViewHolder, position: Int) {
        holder.bindView(getItem(position))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    fun getItem(position: Int): IOfflineMap {
        return items.getOrNull(position)
            ?: throw IllegalArgumentException("There is no item on position: $position")
    }

    fun getItemPosition(item: IOfflineMap): Int {
        return items.indexOfFirst { it.offlineMapId == item.offlineMapId }
    }

    @UiThread
    @Synchronized
    fun setItems(items: MutableList<IOfflineMap>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun notifyItemChanged(item: IOfflineMap) {
        val position = getItemPosition(item)
        notifyItemChanged(position, item)
    }

}