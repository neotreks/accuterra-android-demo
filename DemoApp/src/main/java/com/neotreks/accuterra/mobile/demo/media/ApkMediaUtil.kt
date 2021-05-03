package com.neotreks.accuterra.mobile.demo.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.neotreks.accuterra.mobile.demo.ui.ApkIOUtils
import com.neotreks.accuterra.mobile.demo.util.CrashSupport
import com.neotreks.accuterra.mobile.sdk.trip.model.TripRecordingMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class to handle APK media
 */
class ApkMediaUtil {

    companion object {

        /* * * * * * * * * * * * */
        /*      PROPERTIES       */
        /* * * * * * * * * * * * */

        private const val TAG = "ApkMediaUtil"

        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"


        /* * * * * * * * * * * * */
        /*        PUBLIC         */
        /* * * * * * * * * * * * */

        /**
         * This is not mandatory. Media position is an optional field. If not set, then media
         * are ordered naturally in the same order as there were created.
         *
         * But it is possible to set the order of media to change the natural ordering.
         */
        fun updatePositions(allMedia: MutableList<TripRecordingMedia>): MutableList<TripRecordingMedia> {
            var position = 1
            val ordered = mutableListOf<TripRecordingMedia>()
            for (media in allMedia) {
                ordered.add(
                    // Set the optional position value
                    media.copy(position = position++)
                )
            }
            return ordered
        }

        /**
         * Creates a temporary photo file
         */
        fun createTempCameraFile(context: Context): File {
            val baseFolder = getCameraTempFolder(context)
            val file = File(
                baseFolder, SimpleDateFormat(FILENAME, Locale.US)
                    .format(System.currentTimeMillis()) + PHOTO_EXTENSION
            )
            ApkIOUtils.ensureFilePath(file)
            return file
        }

        /**
         * Creates a temporary photo file and place content of the [bitmap] into this file
         */
        @Suppress("unused")
        suspend fun createTempCameraFile(context: Context, bitmap: Bitmap): File {
            return withContext(Dispatchers.IO) {
                val file = createTempCameraFile(context)
                file.outputStream().use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                }
                return@withContext file
            }
        }

        /**
         * Builds and intent for selecting an image from the device gallery
         */
        fun buildSelectImageIntent(): Intent {
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            val mimeTypes = arrayOf("image/jpeg", "image/png")
            photoPickerIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            return photoPickerIntent
        }

        /**
         * Copies image into devices gallery - into the `AccuTerra` library
         */
        @Suppress("BlockingMethodInNonBlockingContext")
        suspend fun addPictureToTheGallery(context: Context, file: File) {

            withContext(Dispatchers.IO) {

                try {
                    val name = file.nameWithoutExtension

                    val values = ContentValues(4).apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AccuTerra/")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                    }
                    val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: throw IllegalStateException("Cannot create image uri for: $values")

                    val openOutputStream = context.contentResolver.openOutputStream(imageUri)
                        ?: throw IllegalStateException("Cannot create output stream for: $imageUri")
                    openOutputStream.use { out ->
                        file.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(imageUri, values, null, null)
                    } else {
                        // TODO:boronmic REMOVE when compiler is fixes and to not need this else block
                    }

                } catch (e: Exception) {
                    val message = "Error while copying photo into AccuTerra image library: ${file.path}"
                    Log.e(TAG, message, e)
                    CrashSupport.reportError(e, message)
                }
            }
        }

        suspend fun clearCameraTempFolder(context: Context) {
            withContext(Dispatchers.IO) {
                val folder = getCameraTempFolder(context)
                val files = folder.listFiles { _, name ->
                    name.endsWith(PHOTO_EXTENSION, ignoreCase = true)
                }
                for (file in files ?: arrayOf()) {
                    deleteFileSilently(file)
                }
            }
        }

        /**
         * Deletes given file if exists.
         * Does not crash in case of any error.
         * Folders are deleted recursively.
         */
        @Suppress("MemberVisibilityCanBePrivate")
        suspend fun deleteFileSilently(file: File) {
            withContext(Dispatchers.IO) {
                try {
                    if (file.exists()) {
                        when {
                            file.isFile -> file.delete()
                            file.isDirectory -> file.deleteRecursively()
                            else -> Log.d(TAG, "Given file is not file not directory: ${file.path}")
                        }
                    } else {
                        Log.d(TAG, "No file at path: ${file.path}")
                    }
                } catch (e: Exception) {
                    val message = "Error while deleting file '${file.path}'"
                    Log.e(TAG, message, e)
                    CrashSupport.reportError(e, message)
                }
            }
        }

        /* * * * * * * * * * * * */
        /*        PRIVATE        */
        /* * * * * * * * * * * * */

        private fun getCameraTempFolder(context: Context): File {
            val dir = File(context.filesDir, "camera")
            ApkIOUtils.ensureFilePath(dir)
            return dir
        }

    }

}