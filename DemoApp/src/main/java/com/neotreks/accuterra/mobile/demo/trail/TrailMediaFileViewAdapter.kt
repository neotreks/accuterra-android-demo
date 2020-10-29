package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.util.visibility
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailMedia


/**
 * Adapter for trip media
 */
class TrailMediaFileViewAdapter internal constructor(
    context: Context,
    lifecycleScope: LifecycleCoroutineScope,
    private val data: Array<TrailMedia>,
    private val readOnly: Boolean
) :
    RecyclerView.Adapter<TrailMediaFileViewAdapter.ViewHolder?>() {
    
    private var clickListener: ItemClickListener? = null

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    private val viewBinder = TrailMediaFileViewBinder(context, lifecycleScope)

    private val selectedItem: TrailMedia? = null

    // View holder creation
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = inflater.inflate(R.layout.component_image_view, parent, false)
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

    // convenience method for getting data at click position
    fun getItem(id: Int): TrailMedia {
        return data[id]
    }

    // allows clicks events to be caught
    fun setClickListener(itemClickListener: ItemClickListener?) {
        clickListener = itemClickListener
    }

    private fun isSelected(item: TrailMedia): Boolean {
        return item.id == selectedItem?.id
    }

    // Click listener interface
    interface ItemClickListener {
        fun onItemClick(view: View?, position: Int)
        fun onDeleteItemClick(view: View?, position: Int)
    }

}