package com.neotreks.accuterra.mobile.demo.util

/**
 * Utility enum used for toggling list view size.
 */
enum class ListViewSize(val showListViewIcons: Boolean) {

    MINIMUM(false),
    MEDIUM(true),
    MAXIMUM(false);

    fun toggleListView(): ListViewSize {
        return values()[(this.ordinal + 1) % values().size]
    }

    fun toggleMinMaxListView(): ListViewSize {
        return if (this == MINIMUM) {
            MAXIMUM
        } else {
            MINIMUM
        }
    }

}