package com.neotreks.accuterra.mobile.demo.upload

import android.view.View
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.extensions.toIsoDateTimeString
import com.neotreks.accuterra.mobile.demo.ui.ListItemAdapterViewBinder
import com.neotreks.accuterra.mobile.sdk.sync.model.UploadRequest
import kotlinx.android.synthetic.main.upload_request_list_item.view.*
import java.lang.StringBuilder

/**
 * View Binder for the [UploadRequestAdapter]
 */
class UploadRequestViewBinder:
    ListItemAdapterViewBinder<UploadRequest> {

    override fun getViewResourceId(): Int {
        return R.layout.upload_request_list_item
    }

    override fun bindView(view: View, item: UploadRequest, isSelected: Boolean) {
        // Type and priority
        view.upload_request_list_item_type.text = item.dataType.getName()
        view.upload_request_list_item_priority.text = item.priority.getName()
        // Status
        view.upload_request_list_item_status.text = if (item.uploadDate == null) "QUEUED" else "UPLOADED"
        val color = if (item.uploadDate == null) {
                view.context.getColor(R.color.colorAccent)
        } else {
                view.context.getColor(R.color.colorPrimary)
        }
        view.upload_request_list_item_status.setTextColor(color)
        // Info
        view.upload_request_list_item_info.text = buildFullInfo(item)
    }

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        fun buildFullInfo(item: UploadRequest): String {
            val sb = StringBuilder(200)
            sb.appendLine("Data type: ${item.dataType}")
            sb.appendLine("Priority: ${item.priority}")
            sb.appendLine("UUID: ${item.uuid}")
            sb.appendLine("Data UUID: ${item.dataUuid}")
            sb.appendLine("Parent data UUID: ${item.parentUuid ?: ""}")
            sb.appendLine("--")
            sb.appendLine("Failed attempts: ${item.failedAttempts}")
            sb.appendLine("Last attempt date: ${item.lastUploadAttemptDate?.toIsoDateTimeString()}")
            sb.appendLine("Next attempt min. date: ${item.nextAttemptMinDate?.toIsoDateTimeString()}")
            sb.appendLine("Upload date: ${item.uploadDate?.toIsoDateTimeString()}")
            sb.appendLine("Error: ${item.error ?: "" }")
            sb.appendLine("--")
            sb.appendLine("Data path: ${item.dataPath ?: "" }")
            return sb.toString()
        }

    }


}
