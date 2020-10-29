package com.neotreks.accuterra.mobile.demo.trip.location

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import com.neotreks.accuterra.mobile.demo.BuildConfig
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.sdk.location.LocationUpdatesService


/**
 * Basic class for activities used for tracking GPS locations
 *
 * The entry point is the [onLocationUpdated] method where location updates are reported.
 * Also please note [startLocationUpdates], [startLocationRecording] and corresponding stop methods.
 */
abstract class LocationActivity : AppCompatActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    ILocationUpdateListener {

    companion object {
        private const val TAG = "LocationActivity"

        // Used in checking for runtime permissions.
        private const val REQUEST_PERMISSIONS_REQUEST_CODE = 77
    }

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    /**
     * Required location permissions
     */
    private val requiredLocationPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // The BroadcastReceiver used to listen from broadcasts from the service.
    private lateinit var myReceiver: MyReceiver

    // A reference to the service used to get location updates.
    private var mService: LocationService? = null

    // Tracks the bound state of the service.
    private var mBound = false

    // Monitors the state of the connection to the service.
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder =
                service as LocationService.LSLocalBinder
            mService = binder.service
            mBound = true
            onLocationServiceBind()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mBound = false
        }
    }

    /* * * * * * * * * * * * */
    /*        ABSTRACT       */
    /* * * * * * * * * * * * */

    /**
     * Called when location permissions were granted.
     */
    abstract fun onLocationPermissionsGranted()

    /**
     * Invoked when location service is bind with the [LocationActivity]
     */
    abstract fun onLocationServiceBind()

    /** Provide view for displaying the [Snackbar] with info about location permissions */
    abstract fun getViewForSnackbar(): View

    /** Provides last location live data object */
    protected abstract fun getLastLocationLiveData(): MutableLiveData<Location?>

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        myReceiver = MyReceiver()
    }

    override fun onStart() {
        super.onStart()
        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        Intent(this, LocationService::class.java).also { intent ->
            bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            myReceiver,
            IntentFilter(LocationService.ACTION_BROADCAST)
        )
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver)
        super.onPause()
    }

    override fun onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection)
            mBound = false
        }
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Log.i(TAG, "User interaction was cancelled.")
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    // Permission was granted.
                    mService!!.requestLocationUpdates()
                    onLocationPermissionsGranted()
                }
                else -> {
                    // Permission denied.
                    Snackbar.make(
                        getViewForSnackbar(),
                        R.string.location_update_service_permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE
                    )
                        .setAction(R.string.general_settings) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri: Uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID, null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences,
        s: String
    ) {
        // Update the buttons state depending on whether location updates are being requested.
        if (s == LocationUpdatesService.KEY_REQUESTING_LOCATION_UPDATES) {
            val value = LocationUpdatesService.requestingLocationUpdates(this)
            onRequestingLocationUpdatesChange(value)
        }
    }

    /* * * * * * * * * * * * */
    /*        PUBLIC         */
    /* * * * * * * * * * * * */

    open fun onRequestingLocationUpdatesChange(requestingLocationUpdates: Boolean) {
        Log.i(TAG, "Request location updates: $requestingLocationUpdates")
    }

    /* * * * * * * * * * * * */
    /*       PROTECTED       */
    /* * * * * * * * * * * * */

    protected fun getLocationService(): LocationService? {
        return mService
    }

    /**
     * Start reporting location changes.
     */
    protected fun startLocationUpdates() {
        val service = getLocationService()
        checkNotNull(service) { "Location service is expected."}
        service.requestLocationUpdates()
    }

    /**
     * Stops reporting location changes.
     */
    protected fun stopLocationUpdates() {
        val service = getLocationService()
        checkNotNull(service) { "Location service is expected."}
        service.removeLocationUpdates()
    }

    /**
     * Start recording location changes inside of the service.
     */
    protected fun startLocationRecording() {
        val service = getLocationService()
        checkNotNull(service) { "Location service is expected."}
        service.requestLocationRecording()
    }

    /**
     * Stops recording location changes inside of the service.
     */
    protected fun stopLocationRecording() {
        val service = getLocationService()
        checkNotNull(service) { "Location service is expected."}
        service.removeLocationRecording()
    }

    /**
     * Check if APK has locations permissions
     * and if not it will display an info to request these permissions
     */
    protected fun checkLocationPermissions() {
        if (!hasLocationPermissions()) {
            requestPermissions()
        }
    }

    /**
     * Returns the current state of the location permissions needed.
     */
    protected fun hasLocationPermissions(): Boolean {
        Log.v(TAG, "hasLocationPermissions()")

        return requiredLocationPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    protected fun requestPermissions() {
        val shouldProvideRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(
                TAG,
                "Displaying permission rationale to provide additional context."
            )
            Snackbar.make(
                getViewForSnackbar(),
                R.string.location_update_service_permission_rationale,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.general_ok) { // Request permission
                    ActivityCompat.requestPermissions(
                        this@LocationActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                this@LocationActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    /* * * * * * * * * * * * */
    /*     INNER CLASS       */
    /* * * * * * * * * * * * */

    /**
     * Receiver for broadcasts sent by [LocationService].
     */
    private inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val location = intent.getParcelableExtra<Location>(LocationService.EXTRA_LOCATION)
            if (location != null) {
                onLocationUpdated(location)
            }
        }
    }

}