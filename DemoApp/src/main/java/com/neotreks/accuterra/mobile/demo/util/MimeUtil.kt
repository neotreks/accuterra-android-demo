package com.neotreks.accuterra.mobile.demo.util

import android.webkit.MimeTypeMap
import java.io.File

/**
 * Utility class for working with mime types
 */
object MimeUtil {

    /**
     * Returns detected mime type for given [filePath]
     *
     * @param filePath File path or URL
      */
    fun getMimeType(filePath: String?): String? {
        val extension: String = MimeTypeMap.getFileExtensionFromUrl(filePath)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    /**
     * Returns detected mime type for given [file]
     *
     * @param file File
     */
    fun getMimeType(file: File): String? {
        return getMimeType(file.path)
    }

}