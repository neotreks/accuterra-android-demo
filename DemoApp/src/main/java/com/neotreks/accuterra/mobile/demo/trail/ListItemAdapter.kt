package com.neotreks.accuterra.mobile.demo.trail

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.UiThread
import com.neotreks.accuterra.mobile.demo.R

abstract class ListItemAdapter<T>(context: Context,
                                  items: List<T>,
                                  private val viewBinder: ListItemAdapterViewBinder<T>
)
    : ArrayAdapter<T>(context, R.layout.list_item, items) {

    private var listener: OnListItemClickedListener<T>? = null
    private val inflater = LayoutInflater.from(context)

    var selectedItemId: Long? = null

    abstract fun getItemId(item: T): Long

    @UiThread
    @Synchronized
    fun setItems(items: List<T>) {
        clear()
        addAll(items)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: inflater.inflate(viewBinder.getViewResourceId(), parent, false)

        val item = getItem(position)!!

        // Fill the item view with data
        viewBinder.bindView(view, item, selectedItemId == getItemId(item))

        // Attach click handler
        attachClickHandlers(view, item)

        return view
    }

    fun setOnListItemClickedListener(listener: OnListItemClickedListener<T>) {
        this.listener = listener
    }

    private fun attachClickHandlers(view: View, item: T) {
        view.setOnClickListener {
            listener?.onListItemClicked(item)
        }
    }

}

interface OnListItemClickedListener<T> {
    fun onListItemClicked(item: T)
}

interface ListItemAdapterViewBinder<T> {
    fun bindView(view: View, item: T, isSelected: Boolean)
    fun getViewResourceId(): Int
}