package com.neotreks.accuterra.mobile.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.neotreks.accuterra.mobile.demo.security.DemoAccessManager
import com.neotreks.accuterra.mobile.demo.user.DemoIdentityManager
import com.neotreks.accuterra.mobile.demo.util.DialogUtil
import com.neotreks.accuterra.mobile.demo.util.EnumUtil
import com.neotreks.accuterra.mobile.sdk.*
import com.neotreks.accuterra.mobile.sdk.trail.model.NetworkTypeConstraint
import com.neotreks.accuterra.mobile.sdk.trail.model.TrailConfiguration
import com.neotreks.accuterra.mobile.sdk.trail.model.TripConfiguration
import kotlinx.android.synthetic.main.activity_demo.*
import kotlinx.coroutines.*

class DemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DemoActivity"
    }

    private val coroutineExceptionHandler: CoroutineExceptionHandler =
      CoroutineExceptionHandler { _, throwable ->
        lifecycleScope.launch(Dispatchers.Main) {
            DialogUtil.buildOkDialog(this@DemoActivity, getString(R.string.general_error),
            "Error has occurred: ${throwable.localizedMessage}").show()
        }
        GlobalScope.launch { Log.e(TAG, "Caught $throwable", throwable)}
      }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)
        setSupportActionBar(toolbar)

        // Test the reference to the SDK
        Log.i(TAG, "AccuTerra SDK Version: ${SdkInfo().versionName}")

        // Checking if DB is initialized is here just for the DEMO purpose.
        // You should not check this in real APK but call the `SdkManager.initSdk()`
        // and monitor the progress and result. The TRAIL DB will be downloaded automatically
        // during the SDK initialization.
        if (!SdkManager.isTrailDbInitialized(this)) {
            // Display DIALOG
            DialogUtil.buildOkDialog(
                context = this,
                title = "Download",
                message = "The trail DB is going to be downloaded now.",
                code = {
                    // Init the SDK. Since DB was not downloaded yet, it will be downloaded
                    // during SDK initialization.
                    initSdk(
                        // Start Using the SDK
                        onSdkInitSuccess = { onSdkInitSuccess() },
                        // DO NOT USE THE SDK just display the error
                        onSdkInitFailure = { e -> displaySdkInitError(e) }
                    )
                }
            ).show()
        } else {
            // Init the SDK. Since DB was downloaded already there will be no download now.
            initSdk(
                // Start Using the SDK
                onSdkInitSuccess = { onSdkInitSuccess() },
                // DO NOT USE THE SDK just display the error
                onSdkInitFailure = { e -> displaySdkInitError(e) }
            )
        }

    }

    private fun onSdkInitSuccess() {
        // Cache some enumerations and go to the trail discovery activity
        lifecycleScope.launch(coroutineExceptionHandler) {
            EnumUtil.cacheEnums(this@DemoActivity)
            goToTrailDiscovery()

            // Let's resume the upload queue in case there are any upload requests unprocessed
            withContext(Dispatchers.Default) {
                val service = ServiceFactory.getSynchronizationService(applicationContext)
                service.resumeUploadQueue()
            }
        }
    }

    private fun initSdk(onSdkInitSuccess: () -> Unit,
                        onSdkInitFailure: (Throwable?) -> Unit) {
        // Progress Listener for downloading AccuTerra TRAIL DB

        val listener = object : SdkInitListener {
            override fun onStateChanged(state: SdkInitState, detail: SdkInitStateDetail?) {
                lifecycleScope.launch(Dispatchers.Main + coroutineExceptionHandler) {
                    when (state) {
                        // Show progress bar
                        SdkInitState.IN_PROGRESS,
                        SdkInitState.WAITING,
                        SdkInitState.WAITING_FOR_NETWORK -> showProgressBar(detail)
                        // Hide progress bar
                        SdkInitState.CANCELED,
                        SdkInitState.COMPLETED,
                        SdkInitState.FAILED -> hideProgressBar()
                    }
                }
            }

            override fun onProgressChanged(progress: Float) {
                Log.d(TAG, "Progress changed: $progress")
                lifecycleScope.launch(Dispatchers.Main + coroutineExceptionHandler) {
                    activity_demo_progress_bar.progress = (progress * 100F).toInt()
                }
            }

        }

        // Configuring AccuTerra SDK including download of the TRAIL DB if needed.
        lifecycleScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
            // We need to get the valid access token for AccuTerra services
            // Now we create the configuration
            val config = SdkConfig(
                wsUrl = BuildConfig.WS_BASE_URL,
                accuterraMapStyleUrl = BuildConfig.ACCUTERRA_MAP_STYLE_URL,
                tripConfiguration = TripConfiguration(
                    // Just to demonstrate the upload network type constraint
                    uploadNetworkType = NetworkTypeConstraint.CONNECTED,
                    // Let's keep the trip recording on the device for development reasons,
                    // otherwise it should be deleted
                    deleteRecordingAfterUpload = false
                ),
                trailConfiguration = TrailConfiguration(
                    // Update trail DB during SDK initialization
                    updateTrailDbDuringSdkInit = true,
                    // Update trail User Data during SDK initialization
                    updateTrailUserDataDuringSdkInit = true
                )
            )
            // This is the main initialization of the AccuTerra SDK.
            // The listener is notified about progress of the initialization.
            // This initialization must be called and successfully finish at least once
            // (after APK installation) to be able to use the SDK.
            val result = SdkManager.initSdk(applicationContext, config, DemoAccessManager(), DemoIdentityManager(), listener)
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    onSdkInitSuccess()
                } else {
                    Log.e(TAG, "AccuTerra SDK initialization has failed because of: ${result.errorMessage}",
                        result.error)
                    onSdkInitFailure(result.error)
                }
            }
        }
    }

    /**
     * Displays progress barr and progress text for SDK initialization
     */
    private fun showProgressBar(detail: SdkInitStateDetail?) {
        activity_demo_progress_bar.visibility = View.VISIBLE
        activity_demo_progress_bar_text.visibility = View.VISIBLE
        when (detail) {
            // Download
            SdkInitStateDetail.TRAIL_DB_DOWNLOAD -> {
                activity_demo_progress_bar_text.text =
                    getString(R.string.demo_activity_downloading_trail_db)
            }
            // Unpack
            SdkInitStateDetail.TRAIL_DB_UNPACK -> {
                activity_demo_progress_bar_text.text = getString(R.string.demo_activity_unpacking_trail_db)
            }
            // TRAIL DB Update
            SdkInitStateDetail.TRAIL_DB_UPDATE -> {
                activity_demo_progress_bar_text.text = getString(R.string.demo_activity_updating_trail_db)
            }
            // TRAIL USER DATA Update
            SdkInitStateDetail.TRAIL_USER_DATA_UPDATE -> {
                activity_demo_progress_bar_text.text = getString(R.string.demo_activity_updating_trail_user_data)
            }
            // Init Trail Paths
            SdkInitStateDetail.TRAIL_PATHS_CACHE_INIT -> {
                activity_demo_progress_bar_text.text = getString(R.string.demo_activity_init_trail_paths_cache)
            }
            // Init Trail Markers
            SdkInitStateDetail.TRAIL_MARKERS_CACHE_INIT -> {
                activity_demo_progress_bar_text.text = getString(R.string.demo_activity_init_trail_markers_cache)
            }
        }
    }

    private fun hideProgressBar() {
        activity_demo_progress_bar.visibility = View.GONE
        activity_demo_progress_bar_text.visibility = View.GONE
    }

    private fun displaySdkInitError(e: Throwable?) {
        DialogUtil.buildOkDialog(
            context = this@DemoActivity,
            title = "Error",
            message = """   |AccuTerra SDK initialization has failed because of:
                            |   ${e?.message}
                            |   
                            |Do not use the SDK in case of initialization failure!
                      """.trimMargin()
        ).show()
        hideProgressBar()
    }


    private fun goToTrailDiscovery() {
        //TODO Honza: Get rid of the DemoActivity
        startActivity(Intent(this, TrailDiscoveryActivity::class.java))
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_demo, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
