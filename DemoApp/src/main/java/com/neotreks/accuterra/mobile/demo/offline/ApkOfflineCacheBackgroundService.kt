package com.neotreks.accuterra.mobile.demo.offline

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.ServiceConnection
import android.util.Log
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.offlinemap.OfflineMapsActivity
import com.neotreks.accuterra.mobile.sdk.cache.OfflineCacheBackgroundService
import com.neotreks.accuterra.mobile.sdk.util.IErrorHandler

/**
 * Example how we can customize the default [OfflineCacheBackgroundService]
 *
 * You can customize methods like:
 * * [getNotificationChannel]
 * * [getNotificationID]
 * * [getNotificationBuilder]
 * * [getMapCachingNotification]
 *
 */
@Keep
class ApkOfflineCacheBackgroundService: OfflineCacheBackgroundService() {

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun getNotificationBuilder(): NotificationCompat.Builder {

        val builder = super.getNotificationBuilder()

        // APK customize intent
        val intent = Intent(this, ApkOfflineCacheBackgroundService::class.java)

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)

        // The PendingIntent to launch activity.
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, OfflineMapsActivity::class.java), 0
        )

        // Extend Default Builder
        builder
            .addAction(
                R.drawable.ic_flash_on_24px, getString(R.string.location_update_service_go_to_apk),
                activityPendingIntent
            )

        // Put own icon
        builder.setSmallIcon(R.mipmap.ic_launcher)

        return builder
    }

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        private const val TAG = "ApkOfflineCacheService"

        /**
         * Binds this service to Activity
         */
        fun bindService(
            activity: Activity,
            lifecycleScope: LifecycleCoroutineScope,
            serviceConnection: ServiceConnection,
            errorHandler: IErrorHandler? =  null,
        ) {
            bindService(activity, lifecycleScope, serviceConnection, ApkOfflineCacheBackgroundService::class.java, object : IErrorHandler {
                override fun onError(e: Exception) {
                    Log.e(TAG, "Error while binding ${this::class.java.name}")
                    errorHandler?.onError(e)
                }
            })

        }

        /**
         * Unbinds this service to Activity
         */
        fun unbindService(
            activity: Activity,
            serviceConnection: ServiceConnection,
            errorHandler: IErrorHandler? = null
        ) {
            unbindService(activity, serviceConnection, ApkOfflineCacheBackgroundService::class.java,
                object : IErrorHandler {
                    override fun onError(e: Exception) {
                        Log.e(TAG, "Error while unbinding ${this::class.java.name}")
                        errorHandler?.onError(e)
                    }
                })
        }

    }

}