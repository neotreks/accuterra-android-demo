package com.neotreks.accuterra.mobile.demo.trip.online

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.media.ApkMediaVariant
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.trip.model.TripMedia


/**
 * Adapter for trip media
 */
class OnlineTripMediaFileViewAdapter internal constructor(
    context: Context,
    lifecycleScope: LifecycleCoroutineScope,

    private val data: Array<TripMedia>,
    mediaVariant: ApkMediaVariant,
    private val readOnly: Boolean,
    private val imageLayoutResource: Int = R.layout.component_image_view
) : RecyclerView.Adapter<OnlineTripMediaFileViewAdapter.ViewHolder?>() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private var clickListener: ItemClickListener? = null

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    private val viewBinder = OnlineTripMediaFileViewBinder(context, lifecycleScope, imageLayoutResource, mediaVariant)

    private val selectedItem: TripMedia? = null

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    // View holder creation
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = inflater.inflate(imageLayoutResource, parent, false)
        return ViewHolder(view)
    }

    // View binding
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        viewBinder.bindView(holder.itemView, item, isSelected(item))
    }

    // Total number of items
    override fun getItemCount(): Int {
        return data.size
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    // convenience method for getting data at click position
    fun getItem(id: Int): TripMedia {
        return data[id]
    }

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener?) {
        clickListener = itemClickListener
    }

    /**
     * Returns item position based on provided URL.
     * Returns -1 if URL is not found.
     */
    fun getItemPositionBy(url: String): Int {
        val item = data.find { it.url == url }
            ?: return -1
        return data.indexOf(item)
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun isSelected(item: TripMedia): Boolean {
        return item.url == selectedItem?.url
    }

    /* * * * * * * * * * * * */
    /*       INTERNAL        */
    /* * * * * * * * * * * * */

    // Click listener interface
    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
        fun onDeleteItemClick(view: View?, position: Int)
    }

    // View Holder implementation
    inner class ViewHolder internal constructor(itemView: View)
        : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val delete: ImageButton = itemView.findViewById(R.id.image_view_delete)

        init {
            itemView.setOnClickListener(this)
            if (readOnly) {
                delete.visibility = false.visibility
            } else {
                delete.setOnClickListener {
                    onDeleteClicked(itemView, adapterPosition)
                }
            }
        }
        override fun onClick(view: View?) {
            clickListener?.onItemClick(view, adapterPosition)
        }

        private fun onDeleteClicked(view: View, adapterPosition: Int) {
            clickListener?.onDeleteItemClick(view, adapterPosition)
        }
    }

}