package com.neotreks.accuterra.mobile.demo.ui

import java.io.File

/**
 * IO Utility methods
 */
object ApkIOUtils {

    /**
     * Creates directory structure for given [file] if needed.
     */
    fun ensureFilePath(file: File) {
        if (file.exists()) {
            return
        }
        file.parentFile!!.mkdirs()
    }

}