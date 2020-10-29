package com.neotreks.accuterra.mobile.demo.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.neotreks.accuterra.mobile.demo.util.DialogUtil

/**
 * Utility class for objects holding the dialog
 */
class ProgressDialogHolder {

    var dialog: AlertDialog? = null

    /**
     * Displays progress dialog on given [ProgressDialogHolder]
     */
    fun displayProgressDialog(context: Context, message: String) {
        dialog = DialogUtil.buildBlockingProgressDialog(context, message)
        dialog?.show()
    }

    /**
     * Hides progress dialog on given [ProgressDialogHolder]
     */
    fun hideProgressDialog() {
        dialog?.dismiss()
        dialog = null
    }

}