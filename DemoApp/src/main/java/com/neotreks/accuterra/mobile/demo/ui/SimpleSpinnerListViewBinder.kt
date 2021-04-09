package com.neotreks.accuterra.mobile.demo.ui

import android.content.Context
import android.view.View
import android.widget.TextView

/**
 * A simple implementation of the ListItemAdapterViewBinder for the spinner (dropdown) component
 */
open class SimpleSpinnerListViewBinder<T>(private val context: Context): ListItemAdapterViewBinder<T> {

    override fun getViewResourceId(): Int {
        return android.R.layout.simple_spinner_item
    }

    override fun bindView(view: View, item: T, isSelected: Boolean, isFavorite: Boolean) {
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = getItemLabel(item)
    }

    override fun bindDropDownView(view: View, item: T, isSelected: Boolean) {
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = getItemDropDownLabel(item)
    }

    protected open fun getItemLabel(item: T): String {
        return item.toString()
    }

    protected open fun getItemDropDownLabel(item: T): String {
        return getItemLabel(item)
    }

}