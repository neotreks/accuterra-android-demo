package com.neotreks.accuterra.mobile.demo.offlinemap

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neotreks.accuterra.mobile.sdk.map.cache.IOfflineMap
import com.neotreks.accuterra.mobile.sdk.map.cache.IOfflineMapManager
import com.neotreks.accuterra.mobile.sdk.map.cache.OfflineMapType
import kotlinx.coroutines.launch

/**
 * View model for the [OfflineMapsActivity]
 */
class OfflineMapsViewModel: ViewModel() {

    companion object {
        private const val TAG = "OfflineMapsViewModel"
    }

    var listItems = MutableLiveData<MutableList<IOfflineMap>>(mutableListOf())

    fun loadOfflineMaps(context: Context, offlineMapManager: IOfflineMapManager) {
        viewModelScope.launch {
            listItems.value = offlineMapManager.getOfflineMaps().toMutableList()
        }
    }

    fun reset() {
        listItems.value = mutableListOf()
    }

    fun updateItem(offlineMap: IOfflineMap) {
        val index = listItems.value?.let { listItems ->
            val index = listItems.indexOfFirst { it.offlineMapId == offlineMap.offlineMapId }
            if (index > -1) {
                listItems[index] = offlineMap
            }
        }
    }

}