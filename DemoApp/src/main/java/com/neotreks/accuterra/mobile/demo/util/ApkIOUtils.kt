package com.neotreks.accuterra.mobile.demo.util

import android.content.Context
import android.os.Trace
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
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

            FileOutputStream(zipFile).use { fileOutputStream ->
                BufferedOutputStream(fileOutputStream).use { bufferedOutputStream ->
                    ZipOutputStream(bufferedOutputStream).use { out ->
                        val data = ByteArray(bufferSize)
                        for (file in files) {
                            Log.v(TAG, "Adding: $file")
                            FileInputStream(file).use { fileInputStream ->
                                BufferedInputStream(fileInputStream, bufferSize).use { origin ->
                                    val entry = ZipEntry(file.path.substring(file.path.lastIndexOf("/") + 1))
                                    out.putNextEntry(entry)
                                    var count: Int
                                    while (origin.read(data, 0, bufferSize).also { count = it } != -1) {
                                        out.write(data, 0, count)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while creating a $zipFile zip file from: $files")
        }
    }

    /**
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    @Throws(IOException::class)
    fun unzip(zipFilePath: File, destDirectory: String) {

        File(destDirectory).run {
            if (!exists()) {
                mkdirs()
            }
        }

        ZipFile(zipFilePath).use { zip ->

            zip.entries().asSequence().forEach { entry ->

                zip.getInputStream(entry).use { input ->


                    val filePath = destDirectory + File.separator + entry.name

                    if (!entry.isDirectory) {
                        // if the entry is a file, extracts it
                        extractFile(input, filePath)
                    } else {
                        // if the entry is a directory, make the directory
                        val dir = File(filePath)
                        dir.mkdir()
                    }

                }

            }
        }
    }

    fun zipDirectory(inputDirectory: File, outputZipFile: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputZipFile))).use { zos ->
            inputDirectory.walkTopDown().forEach { file ->
                val zipFileName = file.absolutePath.removePrefix(inputDirectory.absolutePath).removePrefix("/")
                val entry = ZipEntry( "$zipFileName${(if (file.isDirectory) "/" else "" )}")
                zos.putNextEntry(entry)
                if (file.isFile) {
                    file.inputStream().copyTo(zos)
                }
            }
        }
    }

    /**
     * Extracts a zip entry (file entry)
     * @param inputStream
     * @param destFilePath
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun extractFile(inputStream: InputStream, destFilePath: String) {
        BufferedOutputStream(FileOutputStream(destFilePath)).use { bos ->
            val bytesIn = ByteArray(4096)
            var read: Int
            while (inputStream.read(bytesIn).also { read = it } != -1) {
                bos.write(bytesIn, 0, read)
            }
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

    /**
     * Read bytes from given [inputStream] using [startIndex] to [endIndex]
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun readBytes(inputStream: InputStream, startIndex: Long, endIndex: Long): ByteArray {

        return withContext(Dispatchers.IO) {
            return@withContext inputStream.use { input ->

                var offset = 0
                var remaining = ((endIndex - startIndex) + 1).also { length ->
                    if (length > Int.MAX_VALUE) throw OutOfMemoryError("File $this is too big ($length bytes) to fit in memory.")
                }.toInt()
                val result = ByteArray(remaining)

                // We need to move to the `start index`
                input.skip(startIndex)

                // Now we read the file part
                while (remaining > 0) {
                    val read = input.read(result, offset, remaining)
                    if (read < 0) {
                        break
                    }
                    remaining -= read
                    offset += read
                }
                if (remaining > 0) {
                    return@use result.copyOf(offset)
                }

                return@use result
            }
        }
    }

}