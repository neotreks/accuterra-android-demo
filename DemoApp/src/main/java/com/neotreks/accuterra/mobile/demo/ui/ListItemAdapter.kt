package com.neotreks.accuterra.mobile.demo.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.view.children
import com.neotreks.accuterra.mobile.demo.R

abstract class ListItemAdapter<T>(context: Context,
                                  private val items: List<T>,
                                  protected val viewBinder: ListItemAdapterViewBinder<T>
)
    : ArrayAdapter<T>(context, R.layout.list_item, items) {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private var listener: OnListItemClickedListener<T>? = null
    private var longListener: OnListItemLongClickListener<T>? = null
    private var actionListener: OnListItemActionClickedListener<T>? = null
    private val inflater = LayoutInflater.from(context)

    /**
     * A setting to avoid using converted views
     */
    protected var doNotConvertViews = false

    var selectedItemId: Long? = null

    /* * * * * * * * * * * * */
    /*       ABSTRACT        */
    /* * * * * * * * * * * * */

    abstract fun getItemId(item: T): Long

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = (if (doNotConvertViews) null else convertView)
            ?: inflater.inflate(viewBinder.getViewResourceId(), parent, false)

        val item = getItem(position)!!

        // Fill the item view with data
        viewBinder.bindView(view, item, selectedItemId == getItemId(item))

        // Attach click handler
        attachClickHandlers(view, item)

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: inflater.inflate(viewBinder.getDropDownViewResourceId(), parent, false)

        val item = getItem(position)!!

        // Fill the item dropdown view with data
        viewBinder.bindDropDownView(view, item, selectedItemId == getItemId(item))

        return view
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    /**
     * Setting the item click listener.
     *
     * There is also option to set the "action" click listener.
     * @see setOnListItemDetailClickedListener
     */
    fun setOnListItemClickedListener(listener: OnListItemClickedListener<T>) {
        this.listener = listener
    }

    /**
     * This listener can be set and used if the [getActionView] is overwritten
     * and some clickable view is provided.
     */
    fun setOnListItemDetailClickedListener(listener: OnListItemActionClickedListener<T>) {
        actionListener = listener
    }

    /**
     * Setting the item long click listener.
     */
    fun setOnListItemLongClickListener(listener: OnListItemLongClickListener<T>) {
        this.longListener = listener
    }

    /**
     * Returns item based on its ID
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun getItemByItemId(id: Long) : T? {
        return items.find { getItemId(it) == id }
    }

    /**
     * Returns item based on its ID
     */
    fun getItemPositionByItemId(itemId: Long) : Int? {
        val item = getItemByItemId(itemId)
            ?: return null
        return getPosition(item)
    }

    /* * * * * * * * * * * * */
    /*       PROTECTED       */
    /* * * * * * * * * * * * */

    /**
     * Intended to provide
     */
    protected open fun getActionView(view: View): View? {
        return null
    }

    protected open fun attachClickHandlers(view: View, item: T) {
        view.setOnClickListener {
            listener?.onListItemClicked(item, view)
        }
        longListener?.let { listener ->
            view.setOnLongClickListener {
                listener.onListItemLongClick(item, view)
                true
            }
        }

        (view as? ViewGroup)?.let { viewGroup ->
            viewGroup.children.iterator().forEach { child ->
                listener?.let { listener ->
                    child.setOnClickListener{
                        listener.onListItemClicked(item, view)
                    }
                }
                longListener?.let { listener ->
                    child.setOnLongClickListener {
                        listener.onListItemLongClick(item, view)
                        true
                    }
                }
            }
        }

        // Attach listener for the `action` view
        getActionView(view)?.setOnClickListener {
            actionListener?.onListItemActionClicked(item)
        }
    }

    /**
     * Returns the selected item index starting with 0.
     * Returns -1 when no item is selected.
     */
    fun getSelectedItemIndex(): Int {
        val id = selectedItemId ?: return -1
        val item = items.find { getItemId(it) == id }
        return getPosition(item)
    }

}

interface OnListItemClickedListener<T> {
    fun onListItemClicked(item: T, view: View)
}

interface OnListItemActionClickedListener<T> {
    fun onListItemActionClicked(item: T)
}

interface OnListItemLongClickListener<T> {
    fun onListItemLongClick(item: T, view: View)
}

interface ListItemAdapterViewBinder<T> {
    fun bindView(view: View, item: T, isSelected: Boolean)
    fun getViewResourceId(): Int
    fun getDropDownViewResourceId(): Int = android.R.layout.simple_spinner_dropdown_item

    /**
     * Used for `spinner` (drop down)
     */
    fun bindDropDownView(view: View, item: T, isSelected: Boolean) {
        // Default implementation should be overwrite in implementation class
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = item.toString()
    }
}