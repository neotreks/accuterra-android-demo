package com.neotreks.accuterra.mobile.demo.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.text.InputFilter
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.extensions.isNotNullNorBlank
import kotlinx.android.synthetic.main.dialog_user_input_text.view.*

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
    fun buildYesNoDialog(context: Context, title: String, message: String,
                         positiveCode: (() -> Unit)? = null,
                         negativeCode: (() -> Unit)? = null,
                         positiveCodeLabel: String? = "Yes",
                         negativeCodeLabel: String? = "No"
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveCodeLabel) { _, _ ->
                positiveCode?.invoke()
            }
            .setNegativeButton(negativeCodeLabel) {_, _ ->
                negativeCode?.invoke()
            }
            .create()
    }

    @SuppressLint("InflateParams")
    fun buildInputDialog(context: Activity, title: String, message: String = "",
                         hint: String? = null,
                         positiveCode: ((String) -> Unit)? = null,
                         negativeCode: ((String) -> Unit)? = null,
                         positiveCodeLabel: String? = null,
                         negativeCodeLabel: String? = null,
                         defaultValue: String? = null,
                         options: InputDialogOptions? = null): AlertDialog {

        // Create input view
        val view = context.layoutInflater.inflate(R.layout.dialog_user_input_text, null)
        val textView = view.dialog_user_input_text_text

        val opt = options ?: InputDialogOptions()
        val minInputLength = opt.minInputLength
        val maxInputLength = opt.maxInputLength
        var inputFilters = opt.inputFilters

        // set max input length
        if (maxInputLength != null) {
            val maxLengthFilter = InputFilter.LengthFilter(maxInputLength)
            if (inputFilters == null) {
                inputFilters = arrayOf(maxLengthFilter)
            } else {
                inputFilters += arrayOf(maxLengthFilter)
            }
        }
        // Set the default value if available
        if (defaultValue.isNotNullNorBlank()) {
            textView.setText(defaultValue)
        }
        if (hint.isNullOrBlank()) {
            if (minInputLength > 0) {
                textView.hint = context.getString(R.string.general_constraint_min_x_chars, minInputLength)
            }
        } else {
            textView.hint = hint
        }

        // Crate dialog
        val builder = AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setView(view)
                .setPositiveButton(positiveCodeLabel ?: context.getString(R.string.general_yes)) { _, _ ->
                    positiveCode?.invoke(textView.text.toString())
                }

        val negativeLabel = negativeCodeLabel ?: context.getString(R.string.general_cancel)
        builder.setNeutralButton(negativeLabel) { _, _ ->
            negativeCode?.invoke(textView.text.toString())
        }

        val dialog = builder.create()

        dialog.setOnShowListener {
            // Display the keyboard
            textView.requestFocus()
            context.showSoftKeyBoard(textView)
        }

        // Handle line params
        options?.lines?.let { lineCount ->
            textView.setLines(lineCount)
            if (lineCount == 1) {
                textView.maxLines = lineCount
                textView.isSingleLine = true
            }
        }

        // Add min length
        textView.addTextChangedListener(TextChangeListener { newValue ->
                val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                positiveButton?.isEnabled = newValue.length >=  minInputLength
            })

        return dialog
    }

    fun buildBlockingProgressDialog(context: Context, title: String): AlertDialog {
        val progressView = View.inflate(context, R.layout.progress_dialog_view, null)
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setView(progressView)
            .setCancelable(false)
            .create()
    }

    fun buildBlockingProgressValueDialog(context: Context, title: String): AlertDialog {
        val progressView = View.inflate(context, R.layout.progress_value_dialog_view, null)
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setView(progressView)
            .setCancelable(false)
            .create()
    }

}