package com.neotreks.accuterra.mobile.demo.ui

import android.content.Context
import androidx.annotation.UiThread

abstract class MutableListItemAdapter<T>(
    context: Context,
    items: MutableList<T>,
    viewBinder: ListItemAdapterViewBinder<T>
) : ListItemAdapter<T>(context, items, viewBinder) {

    @UiThread
    @Synchronized
    fun setItems(items: List<T>) {
        clear()
        addAll(items)
        notifyDataSetChanged()
    }

}