package com.neotreks.accuterra.mobile.demo.util

import android.content.Context
import java.io.File

/**
 * Utility File Logging Class intended just for logging test results.
 *
 * Do not use it for a heavy writes!
 *
 * @param context Application Context
 * @param name File name
 */
class FileLogger(context: Context, val name: String) {

    val file = File(context.filesDir, name)

    fun log(text: String) {
        ensureFileExists()
        file.appendText(text + "\n")
    }

    fun getLogAsList(): List<String> {
        ensureFileExists()
        return file.readLines()
    }

    private fun ensureFileExists() {
        if (!file.exists()) {
            file.createNewFile()
        }
    }

}