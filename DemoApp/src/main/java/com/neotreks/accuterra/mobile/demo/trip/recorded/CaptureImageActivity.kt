package com.neotreks.accuterra.mobile.demo.trip.recorded

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.ImageHeaderParser.UNKNOWN_ORIENTATION
import com.google.common.util.concurrent.ListenableFuture
import com.neotreks.accuterra.mobile.demo.R
import com.neotreks.accuterra.mobile.demo.databinding.ActivityCaptureImageBinding
import com.neotreks.accuterra.mobile.demo.extensions.applyAllWindowInsets
import com.neotreks.accuterra.mobile.demo.loadDrawable
import com.neotreks.accuterra.mobile.demo.longToast
import com.neotreks.accuterra.mobile.demo.media.ApkMediaUtil
import com.neotreks.accuterra.mobile.demo.toast
import com.neotreks.accuterra.mobile.demo.util.PermissionSupport
import java.io.File
import java.util.concurrent.Executors

class CaptureImageActivity : AppCompatActivity() {

    /* * * * * * * * * * * * */
    /*      PROPERTIES       */
    /* * * * * * * * * * * * */

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private lateinit var previewView: PreviewView

    private lateinit var imagePreview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var imageAnalysis: ImageAnalysis

    private lateinit var cameraControl: CameraControl
    private lateinit var cameraInfo: CameraInfo

    private lateinit var cameraCaptureButton: ImageButton
    private lateinit var cameraTorchButton: ImageButton

    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var binding: ActivityCaptureImageBinding

    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == UNKNOWN_ORIENTATION) {
                    return
                }

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageAnalysis.targetRotation = rotation
                imageCapture.targetRotation = rotation
            }
        }
    }

    /* * * * * * * * * * * * */
    /*       COMPANION       */
    /* * * * * * * * * * * * */

    companion object {

        const val RESULT_PATH = "RESULT_PATH"

        private const val TAG = "CaptureImageActivity"

        private const val REQUEST_CODE_PERMISSIONS = 10

        private lateinit var outputDirectory: File

        fun createNavigateToIntent(context: Context): Intent {
            return Intent(context, CaptureImageActivity::class.java)
        }

    }

    /* * * * * * * * * * * * */
    /*       OVERRIDE        */
    /* * * * * * * * * * * * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaptureImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyAllWindowInsets(binding.root)
        previewView = binding.activityCaptureImagePreviewView

        imageCapture = ImageCapture.Builder().apply {
            setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        }.build()

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        // Let's put images into the `files\camera` folder
        outputDirectory = File(this.filesDir, "camera")

        cameraCaptureButton = binding.activityCaptureImageCameraCaptureButton
        cameraTorchButton = binding.activityCaptureImageCameraTorchButton

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        if (allPermissionsGranted()) {
            previewView.post { startCamera() }
            setupButtons()
        } else {
            PermissionSupport.requestCameraPermissions(this, REQUEST_CODE_PERMISSIONS)
        }

    }

    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
    }

    /* * * * * * * * * * * * */
    /*        PRIVATE        */
    /* * * * * * * * * * * * */

    private fun startCamera() {

        imagePreview = Preview.Builder().apply {
            setTargetAspectRatio(AspectRatio.RATIO_16_9)
            setTargetRotation(previewView.display.rotation)
        }.build()

        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imagePreview,
                imageCapture,
                imageAnalysis,
            )
            cameraControl = camera.cameraControl
            cameraInfo = camera.cameraInfo
            setTorchStateObserver()

            previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            imagePreview.setSurfaceProvider(previewView.surfaceProvider)
        }, ContextCompat.getMainExecutor(this))

    }

    private fun takePicture() {

        // Create destination file
        val file = ApkMediaUtil.createTempCameraFile(this)

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptions, executor, object : ImageCapture.OnImageSavedCallback {

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                lifecycleScope.launchWhenCreated {
                    Log.d(TAG, "Photo captured to: ${file.path}")
                    val data = Intent().apply {
                        putExtra(RESULT_PATH, file.path)
                    }
                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                val msg = "Photo capture failed: ${exception.message}"
                lifecycleScope.launchWhenCreated {
                    longToast(msg)
                }
            }

        })
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            PermissionSupport.logCameraPermissionRequested(this)
            if (allPermissionsGranted()) {
                binding.activityCaptureImagePreviewView.post { startCamera() }
            } else {
                toast(getString(R.string.general_permission_not_granted))
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted(): Boolean {
        return PermissionSupport.isCameraPermissionGranted(this)
    }

    private fun setupButtons() {
        cameraCaptureButton.setOnClickListener {
            takePicture()
        }
        cameraTorchButton.setOnClickListener {
            toggleTorch()
        }
    }

    private fun toggleTorch() {
        if (cameraInfo.torchState.value == TorchState.ON) {
            cameraControl.enableTorch(false)
        } else {
            cameraControl.enableTorch(true)
        }
    }

    private fun setTorchStateObserver() {
        cameraInfo.torchState.observe(this, { state ->
            if (state == TorchState.ON) {
                cameraTorchButton.setImageDrawable(
                    loadDrawable(
                        R.drawable.ic_flash_on_24px
                    )
                )
            } else {
                cameraTorchButton.setImageDrawable(
                    loadDrawable(
                        R.drawable.ic_flash_off_24px
                    )
                )
            }
        })
    }
}

