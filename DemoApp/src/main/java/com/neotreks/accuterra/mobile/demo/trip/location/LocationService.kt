package com.neotreks.accuterra.mobile.demo.trip.location

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.trip.recorded.TripRecordingActivity
import com.neotreks.accuterra.mobile.demo.util.toJson
import com.neotreks.accuterra.mobile.sdk.ServiceFactory
import com.neotreks.accuterra.mobile.sdk.cache.BinderServiceProvider
import com.neotreks.accuterra.mobile.sdk.location.LocationUpdatesService
import com.neotreks.accuterra.mobile.sdk.trip.recorder.ITripRecorder
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Service used for gathering location updates, recording these with the [ITripRecorder]
 * and broadcasting these via the [LocalBroadcastManager]
 */
@Keep
class LocationService: LocationUpdatesService() {

    companion object {

        private const val TAG = "LocationService"

        fun requestingLocationUpdates(context: Context): Boolean {
            return LocationUpdatesService.requestingLocationUpdates(context)
        }

        private const val PACKAGE_NAME = "com.neotreks.accuterra.mobile.demo.trip.location"

        /**
         * The name of the channel for notifications.
         */
        private const val CHANNEL_ID = "channel_01"

        const val KEY_REQUESTING_LOCATION_RECORDING = "requesting_location_recording"

        const val ACTION_BROADCAST = "$PACKAGE_NAME.broadcast"
        const val EXTRA_LOCATION = "$PACKAGE_NAME.location"

    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private var binder: BinderServiceProvider<LocationService?>?

    private var isRecordLocations = false

    private var lastReportedLocation: Location? = null

    private lateinit var recorder: ITripRecorder

    /* * * * * * * * * * * * */
    /*      CONSTRUCTOR      */
    /* * * * * * * * * * * * */

    init {
        binder = BinderServiceProvider(this)
    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch {
            recorder = ServiceFactory.getTripRecorder(applicationContext)
            isRecordLocations = requestingLocationRecording()
        }
    }

    /**
     * This is the main method where new locations are reported.
     *
     * This override does two things:
     * - records new location into the ITripRecorded if requested
     * - broadcasts new location for potential listeners
     */
    override fun onLocationUpdated(location: Location) {
        lastReportedLocation = location

        // Record locations if requested
        if (isRecordLocations) {
            lifecycleScope.launch {
                try {
                    recorder.logTrackPoint(
                        location = location,
                        debugString = LocationDebugInfo.build(this@LocationService, location).toJson() // optional debug string
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error while recording location: $location")
                }
            }
        }

        // Notify anyone listening for broadcasts about the new location.
        val intent = Intent(ACTION_BROADCAST)
            .setPackage(packageName)
            .putExtra(EXTRA_LOCATION, location)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
    }

    override fun getNotificationChannelID(): String {
        return CHANNEL_ID
    }

    /**
     * Creates the notification displayed when this service is running in background
     */
    override fun getNotification(): Notification {

        val intent = Intent(this, LocationUpdatesService::class.java)
        val text = getNotificationText()

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)

        // The PendingIntent to launch activity.
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, TripRecordingActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .addAction(
                R.drawable.ic_flash_on_24px, getString(R.string.location_update_service_go_to_apk),
                activityPendingIntent
            )
            .setContentTitle(getNotificationTitle())
            .setContentText(text)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(text)
            .setWhen(System.currentTimeMillis())

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(getNotificationChannelID()) // Channel ID
        }
        return builder.build()
    }

    override fun getBinder(): IBinder? {
        return binder
    }

    override fun onDestroy() {
        binder?.destroy()
        binder = null
        super.onDestroy()
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    /**
     * Starts requesting location updates and publish these
     */
    fun requestLocationRecording() {
        isRecordLocations = true
        setRequestingLocationRecording(this, true)
    }

    /**
     * Stops requesting location updates and stops location update publishing
     */
    fun removeLocationRecording() {
        isRecordLocations = false
        setRequestingLocationRecording(this, false)
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun getNotificationTitle(): String {
        return when {
            requestingLocationRecording() -> {
                getString(R.string.location_update_service_notification_title_trip_recording)
            }
            requestingLocationUpdates(this) -> {
                getString(R.string.location_update_service_notification_title_active)
            }
            else -> {
                getString(R.string.location_update_service_notification_title_inactive)
            }
        }
    }

    private fun getNotificationText(): String {
        return when {
            requestingLocationRecording() -> {
                val tripName = runBlocking {
                    return@runBlocking recorder.getActiveTripRecording()?.tripInfo?.name
                }
                getString(R.string.location_update_service_notification_message_trip_recording, tripName)
            }
            requestingLocationUpdates(this) -> {
                getString(R.string.location_update_service_notification_message_active)
            }
            else -> {
                getString(R.string.location_update_service_notification_message_inactive)
            }
        }
    }

    /**
     * Returns true if requesting location recording, otherwise returns false.
     */
    private fun requestingLocationRecording(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(KEY_REQUESTING_LOCATION_RECORDING, false)
    }

    /**
     * Stores the location recording state in SharedPreferences.
     * @param requestingLocationRecording The location recording state.
     */
    private fun setRequestingLocationRecording(context: Context, requestingLocationRecording: Boolean) {
        isRecordLocations = requestingLocationRecording
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(KEY_REQUESTING_LOCATION_RECORDING, requestingLocationRecording)
            .apply()
    }

}