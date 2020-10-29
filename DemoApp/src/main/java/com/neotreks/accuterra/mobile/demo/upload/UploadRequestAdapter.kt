package com.neotreks.accuterra.mobile.demo.upload

import android.content.Context
import com.neotreks.accuterra.mobile.demo.ui.MutableListItemAdapter
import com.neotreks.accuterra.mobile.sdk.sync.model.UploadRequest

/**
 * Adapter for list of Trips
 */
class UploadRequestAdapter(context: Context, items: MutableList<UploadRequest>)
    : MutableListItemAdapter<UploadRequest>(context, items, UploadRequestViewBinder()) {

    override fun getItemId(item: UploadRequest): Long {
        return item.pk
    }
}