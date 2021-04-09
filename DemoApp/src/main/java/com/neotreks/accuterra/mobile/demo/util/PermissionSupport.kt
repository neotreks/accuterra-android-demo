package com.neotreks.accuterra.mobile.demo.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.toast

/**
 * Utilities related to handling permissions
 */
object PermissionSupport {

    /** Camera permissions for Android 9 and older */
    private val CAMERA_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    /** Camera permissions for Android 10, 11 and higher */
    private val CAMERA_PERMISSIONS_A10 = arrayOf(Manifest.permission.CAMERA)

    /**
     * Returns true if APK has permission to select photo from the gallery.
     */
    fun isSelectPhotoPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // This permission is irrelevant on Android 10+
            return true
        }
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request permission to select photo from the gallery
     *
     * * IMPORTANT: Call the [logSelectPhotoPermissionRequested] in the onRequestPermissionsResult()!
     */
    fun requestSelectPhotoPermission(fragment: Fragment, requestCode: Int) {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (neverAskAgainSelected(fragment.requireActivity(), permission)) {
            fragment.requireActivity().toast(fragment.getString(R.string.general_provide_read_media_permission))
        } else {
            fragment.requestPermissions(arrayOf(permission), requestCode)
        }
    }

    /**
     * Request permission to select photo from the gallery
     *
     * * IMPORTANT: Call the [logSelectPhotoPermissionRequested] in the onRequestPermissionsResult()!
     */
    fun requestSelectPhotoPermission(activity: Activity, requestCode: Int) {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (neverAskAgainSelected(activity, permission)) {
            activity.toast(activity.getString(R.string.general_provide_read_media_permission))
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
        }
    }

    /**
     * Logs that permission to select the photo was requested
     */
    fun logSelectPhotoPermissionRequested(context: Context) {
        logPermissionRequested(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /**
     * Returns true if all camera related permissions are granted
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        val permissions = getCameraPermissions()
        for (permission in permissions) {
            val checkSelfPermission = ContextCompat.checkSelfPermission(
                context, permission
            )
            if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
               return false
            }
        }
        return true
    }

    /**
     * Request all permissions required for taking picture with the Camera
     *
     * * IMPORTANT: Call the [logCameraPermissionRequested] in the onRequestPermissionsResult()!
     */
    fun requestCameraPermissions(fragment: Fragment, requestCode: Int) {
        if (neverAskAgainSelected(fragment.requireActivity(), Manifest.permission.CAMERA)) {
            fragment.requireActivity().toast(fragment.getString(R.string.general_provide_camera_permission))
            return
        }
        val permissions = getCameraPermissions()
        fragment.requestPermissions(permissions, requestCode)
    }

    /**
     * Request all permissions required for taking picture with the Camera
     *
     * * IMPORTANT: Call the [logCameraPermissionRequested] in the onRequestPermissionsResult()!
     */
    fun requestCameraPermissions(activity: Activity, requestCode: Int) {
        if (neverAskAgainSelected(activity, Manifest.permission.CAMERA)) {
            activity.toast(activity.getString(R.string.general_provide_camera_permission))
            return
        }
        val permissions = getCameraPermissions()
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * Logs that camera permission was requested
     */
    fun logCameraPermissionRequested(context: Context) {
        logPermissionRequested(context, Manifest.permission.CAMERA)
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun getCameraPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 and newer (e.g. 11)
            CAMERA_PERMISSIONS_A10
        } else {
            // Android 9 and older (e.g. 8.1)
            CAMERA_PERMISSIONS
        }
    }

    /**
     * Return true if the `never ask again` for given permission is detected.
     *
     * Based on the: android.app.Activity#shouldShowRequestPermissionRationale
     * This method returns true if the user has previously denied the request.
     * But if the user has selected “Dont’ ask again” this method would always false.
     * It also returns false if we are prompting for permission for the first time.
     */
    private fun neverAskAgainSelected(activity: Activity, permission: String): Boolean {
        val permissionWasRequested = getPermissionWasRequested(activity, permission)
        val currShouldShowStatus = activity.shouldShowRequestPermissionRationale(permission)
        return permissionWasRequested && !currShouldShowStatus
    }

    /**
     * Write flag that given permission was requested (without respect to the result)
     */
    private fun logPermissionRequested(context: Context, permission: String) {
        val genPrefs = context.getSharedPreferences("PERM_UTIL_PREFERENCES", Context.MODE_PRIVATE)
        val editor = genPrefs.edit()
        editor.putBoolean(permission, true)
        editor.apply()
    }

    /**
     * Returns true if given permission was requested at least once
     */
    private fun getPermissionWasRequested(context: Context, permission: String): Boolean {
        val genPrefs = context.getSharedPreferences("PERM_UTIL_PREFERENCES", Context.MODE_PRIVATE)
        return genPrefs.getBoolean(permission, false)
    }

}