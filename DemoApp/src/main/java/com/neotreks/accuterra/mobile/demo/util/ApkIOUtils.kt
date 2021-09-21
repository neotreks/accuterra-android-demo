package com.neotreks.accuterra.mobile.demo.util

import android.content.Context
import android.os.Trace
import android.util.Log
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * IO related utilities
 */
object ApkIOUtils {

    private const val TAG = "ApkIOUtils"

    /**
     * Creates directory structure for given [file] if needed.
     */
    fun ensureFilePath(file: File) {
        if (file.exists()) {
            return
        }
        file.parentFile!!.mkdirs()
    }

    fun copyDatabase(context: Context, dbName: String, destinationFolder: File) {
        val files = getDBFiles(context, dbName, onlyExisting = true)
        // Copy files from `databases` folder into `files` folder
        for (file in files) {
            copyDatabaseFile(context, file.name, destinationFolder)
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun copyDatabaseFile(context: Context, dbFileName: String, destinationFile: File) {

        val dbFile = context.getDatabasePath(dbFileName)
        if (!dbFile.exists()) {
            return
        }

        val destination = File(destinationFile, dbFileName)
        copyFiles(dbFile, destination, overwrite = true)
    }

    fun zip(files: List<File>, zipFile: File, bufferSize: Int = 1024) {
        try {
            // Make Path
            ensureFilePath(zipFile)

            var origin: BufferedInputStream?
            val dest = FileOutputStream(zipFile)
            val out = ZipOutputStream(
                BufferedOutputStream(
                    dest
                )
            )
            val data = ByteArray(bufferSize)
            // copy files
            for (file in files) {
                Log.v(TAG, "Adding: $file")
                val fi = FileInputStream(file)
                origin = BufferedInputStream(fi, bufferSize)
                val entry = ZipEntry(file.path.substring(file.path.lastIndexOf("/") + 1))
                out.putNextEntry(entry)
                var count: Int
                while (origin.read(data, 0, bufferSize).also { count = it } != -1) {
                    out.write(data, 0, count)
                }
                origin.close()
            }
            out.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error while creating a $zipFile zip file from: $files")
        }
    }

    /**
     * Copies given [source] file into [target] location
     */
    @Suppress("unused")
    fun copyFiles(source: File, target: File, overwrite: Boolean = true) {
        if (!source.exists()) {
            throw IOException("Source file does not exists on path: ${source.path}")
        }
        Trace.beginSection("IOUtils#copyFiles - File")
        ensureFilePath(target)
        source.copyTo(target, overwrite)
        Trace.endSection()
    }

    fun deleteDbFiles(context: Context, dbName: String, folder: File? = null) {
        val files = getDBFiles(context, dbName, onlyExisting = true, folder)
        for (file in files) {
            deleteFileSilently(file)
        }
    }

    fun deleteFileSilently(file: File) {
        try {
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Cannot delete file: $file", e)
        }
    }

    fun getDBFiles(
        context: Context,
        dbName: String,
        onlyExisting: Boolean = true,
        /**
         * A root folder. If null then the default DB folder is used.
         */
        folder: File? = null,
    ): List<File> {

        // Temporary files
        val dbName01shm = "$dbName-shm"
        val dbName02wal = "$dbName-wal"
        val dbName03journal = "$dbName-journal"

        val dbFolder = folder ?: context.getDatabasePath(dbName).parentFile

        // Create a ZIP
        val files = listOf(
            File(dbFolder, dbName),
            File(dbFolder, dbName01shm),
            File(dbFolder, dbName02wal),
            File(dbFolder, dbName03journal),
        ).filter {
            if (onlyExisting) {
                it.exists()
            } else {
                true
            }
        }

        return files
    }

}