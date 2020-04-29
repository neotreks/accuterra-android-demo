package com.neotreks.accuterra.mobile.demo.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.neotreks.accuterra.mobile.demo.R

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

    fun buildYesNoDialog(context: Context,
                         title: String,
                         message: String,
                         yesButtonResourceId: Int = R.string.yes,
                         noButtonResourceId: Int = R.string.no,
                      code: (() -> Unit)? = null): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(context.getString(yesButtonResourceId)) { _, _ ->
                code?.invoke()
            }
            .setNegativeButton(context.getString(noButtonResourceId)) { dialog, _ ->
                dialog.cancel()
            }
            .create()
    }
}