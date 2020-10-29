package com.neotreks.accuterra.mobile.demo.extensions

import androidx.lifecycle.MutableLiveData

/**
 * Extensions for `live data` related functionality
 */


/**
 * Utility method to notify observers without changing the value.
 * This is useful e.g. for [MutableList] when the list is modified
 * but the [MutableLiveData.postValue] nor [MutableLiveData.setValue] is not called.
 */
fun <T> MutableLiveData<T>.notifyObserver() {
    this.value = this.value
}