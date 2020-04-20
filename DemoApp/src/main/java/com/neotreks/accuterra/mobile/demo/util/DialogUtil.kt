package com.neotreks.accuterra.mobile.demo.util

import android.content.Context
import androidx.appcompat.app.AlertDialog

/**
 * Utility class to display dialogs
 */
object DialogUtil {


    fun buildOkDialog(context: Context, title: String, message: String,
                      code: (() -> Unit)? = null): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                code?.invoke()
            }
            .create()
    }


}